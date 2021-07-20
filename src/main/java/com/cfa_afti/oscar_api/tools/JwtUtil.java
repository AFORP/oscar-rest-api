/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.tools;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class JwtUtil
{
	private static Logger LOGGER = Logger.getLogger(JwtUtil.class.getName());

	public static String createJwtToken(JWTAuth jwtProvider, int userId, int expiration, JsonObject customData)
	{
		List<String> audienceList = new ArrayList<>();
		audienceList.add("AFTI's users");

		String token = jwtProvider.generateToken(
			customData,
			new JWTOptions()
				.setAlgorithm("RS256")
				.setIssuer("AFTI authentication server")
				.setSubject(String.valueOf(userId))
				.setAudience(audienceList)
				.setExpiresInMinutes(expiration)
		);

		return token;
	}

	public static String[] retrieveJwt(RoutingContext rc)
	{
		String[] secretStrings = new String[3];

		//WEB
		Cookie jwtTokenCookie;
		Cookie jwtRefreshTokenCookie;
		Cookie csrfTokenCookie;

		//Mobile
		String jwtToken;
		String jwtRefreshToken;

		JsonObject jwtTokenJson = null;

		if ((jwtTokenCookie = rc.request().getCookie("jwt_token")) != null
			&& (jwtRefreshTokenCookie = rc.request().getCookie("jwt_refresh_token"))
			!= null /*
			 * && (csrfTokenCookie = rc.request().getCookie("csrf_token")) !=
			 * null
			 */)//WEB
		{

			secretStrings[0] = jwtTokenCookie.getValue();
			secretStrings[1] = jwtRefreshTokenCookie.getValue();
			//secretStrings[2] = rc.request().getCookie("csrf_token").getValue();
		}
		else if (rc.request().getHeader(HttpHeaders.AUTHORIZATION) != null)//Mobile
		{
			secretStrings[0] = rc.request().getHeader(HttpHeaders.AUTHORIZATION);
			jwtTokenJson = JWT.parse(rc.request().getHeader(HttpHeaders.AUTHORIZATION));
			System.out.println("Le JWT : " + jwtTokenJson.toString());
		}
		else
		{
			rc.response().setStatusCode(401);
			LOGGER.warning("The authentication method has not been recognized.");
		}

		return secretStrings;
	}

	public static Future<String> refreshJwt()
	{
		Promise<String> promise = Promise.promise();

		return promise.future();
	}
}
