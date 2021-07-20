/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.auth;

import com.cfa_afti.oscar_api.tools.JsonResponse;
import com.cfa_afti.oscar_api.tools.JwtUtil;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import java.util.logging.Logger;

public class BasicAuth
	implements Handler<RoutingContext>
{
	private static final Logger LOGGER = Logger.getLogger(BasicAuth.class.toString());

	private Vertx vertx;
	private JWTAuth jwtProvider;

	private String email;
	private String password;
	private String redirectURL;

	public BasicAuth()
	{
	}

	public BasicAuth(Vertx vertx, JWTAuth jwtProvider)
	{
		this.vertx = vertx;
		this.jwtProvider = jwtProvider;
	}

	@Override
	public void handle(RoutingContext rc)
	{
		try
		{
			System.out.println(">>> Authorization : "
				+ rc.request().getHeader("Authorization"));

			JsonResponse jsonResponse = new JsonResponse();
			System.out.println(rc.user().principal().toString());

			getUserInfos(rc.user().principal().getString("username"))
				.onSuccess((receivedJson) ->
				{
					JsonObject jsonUserInfos = receivedJson.getJsonObject(0);

					JsonObject customJwtTokenData = new JsonObject();
					JsonObject customJwtRefreshTokenData = new JsonObject();

					customJwtTokenData
						.put("typ", "Bearer")
						.put("group", jsonUserInfos.getString("id_role"))
						//.put("email", Tools.obfuscateEmail(jsonUserInfos.getString("email")))
						.put("scope", "user");

					customJwtRefreshTokenData.put("typ", "Refresh");

					String jwtToken = JwtUtil.createJwtToken(
						jwtProvider,
						jsonUserInfos.getInteger("id_user"),
						30,
						customJwtTokenData);
					String jwtRefreshToken = JwtUtil.createJwtToken(
						jwtProvider,
						jsonUserInfos.getInteger("id_user"),
						3600 * 24 * 30,
						customJwtRefreshTokenData);

					jsonResponse.setStatus("SUCCESS");
					jsonResponse.setHttpCode(rc.response().getStatusCode());
					jsonResponse.getResult()
						.put("user_id", jsonUserInfos.getInteger("id_user"))
						.put("account_type", jsonUserInfos.getString("account_type"))
						//						.put("user_email", Tools.obfuscateEmail(jsonUserInfos.getString("email")))
						.put("jwt_token", jwtToken)
						.put("jwt_refresh_token", jwtRefreshToken);

					rc.response().addCookie(Cookie.cookie("jwt_token", jwtToken)
						.setSecure(true)
						.setHttpOnly(true));
					rc.response().addCookie(Cookie.cookie("jwt_refresh_token", jwtRefreshToken) //"jwt_token", jwtToken
						.setSecure(true)
						.setHttpOnly(true));

					System.out.println(jsonResponse.toString());

					if ((this.redirectURL = rc.request().getParam("redirect"))
						!= null)
					{
						rc.redirect(this.redirectURL);
					}

					else
					{
						LOGGER.info("No redirect URL.");
						rc.response().end(jsonResponse.toString());
					}

				})
				.onFailure((e) ->
				{
					//routingContext.response().setStatusCode(401);
					jsonResponse.setStatus("FAILED");
					jsonResponse.getMessages().add("Incorect email or password.");
					LOGGER.severe("Impossible de retrouver les données de l'utilisateur depuis la Base de Données. Cause : "
						+ e.getMessage());
				});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * Retrouve les informations d'un utilisateur selon son courriel dans la
	 * Base de Données.
	 *
	 * @param username L'email de l'utilisateur.
	 *
	 * @return Retourne un JsonObject contenant les données de l'utilisateur.
	 */
	public Future<JsonArray> getUserInfos(String username)
	{
		Promise<JsonArray> promise = Promise.promise();

		String sqlQuery = "SELECT * FROM Users WHERE username_user=?;";

		this.vertx.eventBus().request(
			"sql_queries",
			new JsonObject()
				.put("query", sqlQuery)
				.put("query_params", new JsonArray().add(username)))
			.onSuccess((msg) ->
			{
				System.out.println(msg.body());
				promise.complete((JsonArray) msg.body());
			})
			.onFailure((event) ->
				promise.fail(String.format("La requête \"%s\" a échoué avec la valeur \"%s\".", sqlQuery, username)));

		return promise.future();
	}
}
