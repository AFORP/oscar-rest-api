/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.v1;

import com.cfa_afti.oscar_api.tools.JsonResponse;
import com.cfa_afti.oscar_api.tools.JwtUtil;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;

/**
 *
 * @author AlliageSphere
 */
public abstract class ApiController
	implements Handler<RoutingContext>
{
//	protected RoutingContext rc;
//
	protected Vertx vertx;
	protected JWTAuth jwtProvider;
	protected MySQLPool mySqlClient;

//	protected SQLClient sqlClient;
	protected JsonResponse jsonResponse;//Reponse de la page.
//	protected JsonObject jsonSqlResult;//Reponse de la requête SQL.
//	protected ArrayList<String> sqlQueries;
//	protected String sqlQuery;
//	protected JsonArray sqlParams;
//	protected int userId;
	protected User user;

	public ApiController()
	{
	}

	public ApiController(Vertx vertx, JWTAuth jwtProvider)
	{
		this.vertx = vertx;
		this.jwtProvider = jwtProvider;
		this.jsonResponse = new JsonResponse();
	}

	@Override
	public void handle(RoutingContext rc)
	{
		try
		{
			rc.response().putHeader("Content-Type", "application/json");
			String[] secretStrings;

			if ((secretStrings = JwtUtil.retrieveJwt(rc)) != null)
			{
				this.jwtProvider.authenticate(new JsonObject().put("token", secretStrings[0]))
					.onSuccess((usr) ->
					{
						this.user = usr;
						System.out.println(">>> " + user.principal().toString());
						System.out.println(">>> " + user.attributes().toString());

						switch (rc.request().method().name())
						{
//							case "GET" ->
//								getMethods(rc).result();
//							case "POST" ->
//								addMethods();
//							case "DELETE" ->
//								deleteMethods();
//							default ->
//								System.err.println("HTTP method not handled here.");
						}
					})
					.onFailure((event) ->
					{
						this.jsonResponse.getMessages().add("Authentication failed - "
							+ event.getMessage());
						//LOGGER.severe(event.getMessage());
						rc.end(this.jsonResponse.toString());
					});
			}
			else
			{
				this.jsonResponse.getMessages().add("JWT not found");
				rc.end(this.jsonResponse.toString());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Code à redéfinir selon ce que vous voulez que la page fasse.
	 */
	//protected abstract void specificCodeEndpoint();
	/**
	 * Permet de gérer les requête HTTP GET.
	 *
	 * @param json Les données à insérer dans la requête pour obtenir la/les
	 *             donnée(s) voulue(s).
	 */
	protected abstract Future<JsonObject> getMethods(RoutingContext rc);
//protected abstract void getMethods(JsonObject json);
	/**
	 * Permet de gérer les requête HTTP DELETE.
	 *
	 * @param json Les données à insérer dans la requête pour supprimer la/les
	 *             donnée(s) voulue(s).
	 */
	protected abstract void deleteMethods();

	/**
	 * Permet de gérer les requête HTTP POST.
	 *
	 * @param json Les données à insérer dans la requête pour insérer la/les
	 *             donnée(s) voulue(s).
	 */
	protected abstract void addMethods();
}
