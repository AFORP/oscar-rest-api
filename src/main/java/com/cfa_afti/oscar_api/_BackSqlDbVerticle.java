/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api;

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

public class _BackSqlDbVerticle
	extends AbstractVerticle
{
  private static Logger LOGGER = Logger.getLogger(_BackSqlDbVerticle.class.getName());
  public static SqlAuthentication sqlProvider;
  private MySQLPool sqlClient;

  public _BackSqlDbVerticle()
  {
  }

  @Override
  public void start(Promise<Void> startPromise)
	  throws Exception
  {
	this.sqlClient = configureMariaDbAccess();
	_BackSqlDbVerticle.sqlProvider = configureSQLAuth(this.sqlClient);

	//Réception d'un objet JSON : {"query":"SELECT * ...;", "query_params":[..., ..., ...]}
	this.vertx.eventBus()
		.consumer("sql_queries").handler((msg) ->
	{
	  handleQuery((JsonObject) msg.body())
		  .onSuccess((rowSet) ->
		  {
			JsonArray resultJsonQuery = new JsonArray();
			JsonObject currentJsonRow;

			RowSet<Row> rows = rowSet;
			for (Row row : rows)
			{
			  currentJsonRow = new JsonObject(row.toJson().toString());
			  resultJsonQuery.add(currentJsonRow);
			}

			//System.out.println(resultJsonQuery.toString());
			msg.reply(resultJsonQuery);
		  })
		  .onFailure((event) ->
		  {
			System.out.println(event.getMessage());
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
  private Future<RowSet> handleQuery(JsonObject queryData)
  {
	Promise promise = Promise.promise();

	//Vérification des données présentes
	if (queryData.containsKey("query"))
	{
	  if (queryData.containsKey("query_params"))
	  {
		System.out.println("Requête préparée détectée.");
		preparedQuery(queryData.getString("query"), queryData.getJsonArray("query_params"))
			.onSuccess((rowSet) ->
				promise.complete(rowSet))
			.onFailure((event) ->
				System.out.println(event.getMessage()));
	  }
	  else
	  {
		System.out.println("Requête simple détectée.");
		simpleQuery(queryData.getString("query"))
			.onSuccess((rowSet) ->
			{
			  System.out.println("1");
			  promise.complete(rowSet);
			})
			.onFailure((event) ->
			{
			  System.out.println(event.getMessage());
			});

		System.out.println("2");
	  }
	}
	else
	{
	  promise.fail("La clef \"query\" est introuvable. Elle est obligatoire !");
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
	return MySQLPool.pool(
		this.vertx,
		new MySQLConnectOptions()
			.setPort(3306)
			.setHost("217.64.58.136")//217.64.58.136 (adresse externe)- 192.168.1.202 (serveur local)
			.setDatabase("oscar_dev")
			.setUser("user-bdd")
			.setPassword("bddpassword"),
		//				.setReconnectAttempts(2)
		//				.setReconnectInterval(5000),
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
