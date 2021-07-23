/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api;

import com.cfa_afti.oscar_api.tools.JsonQueryResponse;
import static com.cfa_afti.oscar_api.MainVerticle.API_CONFIG;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.sqlclient.SqlAuthentication;
import io.vertx.ext.auth.sqlclient.SqlAuthenticationOptions;
import io.vertx.mysqlclient.*;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SqlDbVerticle
	extends AbstractVerticle
{
	private static final Logger LOGGER = Logger.getLogger(SqlDbVerticle.class.getName());
	public static SqlAuthentication sqlProvider;
	private MySQLPool sqlClient;

	public SqlDbVerticle()
	{
	}

	@Override
	public void start(Promise<Void> startPromise)
		throws Exception
	{
		this.sqlClient = configureMariaDbAccess();
		SqlDbVerticle.sqlProvider = configureSQLAuth(this.sqlClient);

		//Réception d'un objet JSON : {"query":"SELECT * ...;", "query_params":[..., ..., ...]}
		//Après une requête SQL : Renvoi d'un objet json : { "status":"SUCCESS/FAILURE",	"messages":[],	"data":[{Ligne 1}, {Ligne 2}] }

		/*
		 * Permet aux différentes parties du programme de communiquer entre elles.
		 * Elle communiquent via un bus d'événement.
		 *
		 */
		this.vertx.eventBus()
			.consumer("sql_queries").handler((msg) ->
		{
			handleQuery((JsonObject) msg.body())
				.onSuccess((jsonResponse) ->
				{
					msg.reply(jsonResponse);
				})
				.onFailure((event) ->
				{
					LOGGER.warning(event.getMessage());
				});
		});
		startPromise.complete();
	}

	/**
	 *
	 * @param queryData Doit contenir "query" qui est la requête à exécuter.<br>
	 * Et un JsonArray (si nécéssaire) qui doit contenir les valeurs de la
	 * requête préparée.
	 *
	 * @return
	 */
	private Future<JsonQueryResponse> handleQuery(JsonObject queryData)
	{
		Promise promise = Promise.promise();
		//JsonObject jsonResp = new JsonObject();
		JsonQueryResponse jQueryResp = new JsonQueryResponse();

		JsonArray resultJsonQuery = new JsonArray();

		//Vérification des données présentes
		if (!queryData.containsKey("query"))
		{
			promise.fail("La clef \"query\" est introuvable. Elle est obligatoire !");
			return promise.future();
		}

		if (queryData.containsKey("query_params"))
		{
			preparedQuery(queryData.getString("query"), queryData.getJsonArray("query_params"))
				.onSuccess((rowSet) ->
				{
					RowSet<Row> rows = rowSet;
					JsonObject currentJsonRow;

					for (Row row : rows)
					{
						currentJsonRow = new JsonObject(row.toJson().toString());
						resultJsonQuery.add(currentJsonRow);
					}

					jQueryResp
						.setStatus("SUCCESS")
						.setData(resultJsonQuery)
						.setRowCount(rows.size());

					promise.complete(jQueryResp);
				})
				.onFailure((event) ->
				{
					jQueryResp
						.setStatus("FAILED")
						.setMessage(event.getMessage());

					System.out.println(event.getMessage());
					promise.complete(jQueryResp);
				});
		}
		else
		{
			simpleQuery(queryData.getString("query"))
				.onSuccess((rowSet) ->
				{
					RowSet<Row> rows = rowSet;
					JsonObject currentJsonRow;

					for (Row row : rows)
					{
						currentJsonRow = new JsonObject(row.toJson().toString());
						resultJsonQuery.add(currentJsonRow);
					}

					jQueryResp
						.setStatus("SUCCESS")
						.setData(resultJsonQuery)
						.setRowCount(rows.size());

					promise.complete(jQueryResp);
				})
				.onFailure((event) ->
				{
					jQueryResp
						.setStatus("FAILED")
						.setMessage(event.getMessage());

					System.out.println(event.getMessage());
					promise.complete(jQueryResp);
				});
		}

		return promise.future();
	}

	private Future<RowSet> preparedQuery(String sqlQuery, JsonArray sqlParams)
	{
		Promise promise = Promise.promise();

		this.sqlClient.preparedQuery(sqlQuery)
			.execute(Tuple.from(sqlParams.getList()))
			.onSuccess((rowSet) ->
			{
				promise.complete(rowSet);//this.sqlClient.close();
			})
			.onFailure((event) ->
			{
				promise.fail(
					String.format("The query \"%s\" has failed with the following parameters : %s.\nThe error is : %s.",
						sqlQuery, sqlParams, event.getMessage()));
			});

		return promise.future();
	}

	private Future<RowSet> simpleQuery(String sqlQuery)
	{
		Promise promise = Promise.promise();

		this.sqlClient.query(sqlQuery)
			.execute()
			.onSuccess((rowSet) ->
			{
				promise.complete(rowSet);//this.sqlClient.close();
			})
			.onFailure((event) ->
			{
				promise.fail(
					String.format("The query \"%s\" has failed because of the following error : %s",
						sqlQuery, event.getMessage()));
			});

		return promise.future();
	}

	private MySQLPool configureMariaDbAccess()
	{
		if(API_CONFIG.getString("bdd.user").equals("root"))
		{
			LOGGER.warning("L'utilisation de l'utilisateur 'root' est à proscrire.");
		}

		return MySQLPool.pool(
			this.vertx,
			new MySQLConnectOptions()
				.setPort(API_CONFIG.getInteger("bdd.port"))
				.setHost(API_CONFIG.getString("bdd.host"))
				.setDatabase(API_CONFIG.getString("bdd.name"))
				.setUser(API_CONFIG.getString("bdd.user"))
				.setPassword(API_CONFIG.getString("bdd.password")),
			new PoolOptions()
				.setMaxSize(6)
				.setIdleTimeout(10)
				.setIdleTimeoutUnit(TimeUnit.SECONDS));
	}

	private SqlAuthentication configureSQLAuth(MySQLPool sqlClient)
	{
		SqlAuthenticationOptions options = new SqlAuthenticationOptions();
		options.setAuthenticationQuery("SELECT password_user FROM Users WHERE username_user=?;");
		return SqlAuthentication.create(sqlClient, options);
	}
}
