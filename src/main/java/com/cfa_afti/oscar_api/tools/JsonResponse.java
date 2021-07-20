/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.tools;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonResponse
{
	private final JsonObject body;//Le message JSON complet.
	private int httpCode;//HTTP Code

	public JsonResponse()
	{
		this.body = new JsonObject();
		//this.messages = new JsonArray();
		//this.result = new JsonObject();

		this.body
			.put("timestamp", System.currentTimeMillis())
			.put("status", "")
			.put("code", 0)
			.put("messages", new JsonArray())
			.put("result", new JsonObject());
	}

	/**
	 * Objet JsonResponse surchargé.
	 *
	 * @param status   S'il y a eu une erreur (SUCCESS/FAILURE).
	 * @param httpCode Code HTTP de la réponse.
	 * @param messages Message utiles pour le débogage.
	 * @param result   Le résultat d'une requête. Les données reçues.
	 */
	/*
	 * public JsonResponse(String status, short httpCode, JsonArray messages, JsonObject result)
	 * {
	 * this.httpCode = httpCode;
	 *
	 * if (result == null)
	 * {
	 * this.result = new JsonObject();
	 * }
	 *
	 * if (this.body == null)
	 * {
	 * this.body
	 * .put("timestamp", System.currentTimeMillis())
	 * .put("status", status)
	 * .put("code", httpCode)
	 * .put("messages", messages)
	 * .put("result", result);
	 * }
	 * }
	 */
	/**
	 * Retourne le statut de la réponse (SUCCESS/FAILURE).
	 *
	 * @return Retourne une chaine de caractères.
	 */
	public String getStatus()
	{
		return this.body.getString("status");
	}

	/**
	 * Retourne le code HTTP de la réponse.
	 *
	 * @return Retourne un JsonObject.
	 */
	public int getHttpCode()
	{
		return this.httpCode;
	}

	/**
	 * Retourne les messages utiles pour le débogage.
	 *
	 * @return Retourne un JsonObject.
	 */
	public JsonArray getMessages()
	{
		return this.body.getJsonArray("messages");
	}

	/**
	 * Retourne le résultat de la requête. Les données reçues.
	 *
	 * @return Retourne un JsonObject.
	 */
	public JsonObject getResult()
	{
		return this.body.getJsonObject("result");
	}

	/**
	 * Représente l'état de la réponse.<br>
	 * Si tout s'est bien déroulé : SUCCESS.<br>
	 * S'il y a eu une/des erreur(s) : FAILED.
	 *
	 * @param status Une chaine de caractères (SUCCESS/FAILED).
	 */
	public void setStatus(String status)
	{
		this.body.put("status", status);
		//this.status = status;
	}

	/**
	 * Définit le code HTTP retourné par la réponse.
	 *
	 * @param httpCode Un short représentant un code HTTP.
	 *
	 * @return Un objet représentant this. Donc la classe peut être utilisée en
	 *         utilisant le Fluent Design.
	 */
	public JsonResponse setHttpCode(int httpCode)
	{
		this.body.put("code", httpCode);
		return this;
	}

	/**
	 * Définit les messages de débogage/d'aide utiles pour l'utilisateur et/ou
	 * le développeur.
	 *
	 * @param messages Un objet JSON représentant les messages.
	 *
	 * @return Un objet représentant this. Donc la classe peut être utilisée en
	 *         utilisant le Fluent Design.
	 */
	public JsonResponse setMessages(JsonObject messages)
	{
		this.body.put("messages", messages);
		return this;
	}

	/**
	 * Définit le résultat de la requête.
	 *
	 * @param result Un objet JSON représentant les données demandées par
	 *               l'utilisateur.
	 *
	 * @return Un objet représentant this. Donc la classe peut être utilisée en
	 *         utilisant le Fluent Design.
	 */
	public JsonResponse setResult(JsonObject result)
	{
		this.body.put("result", result);
		return this;
	}

	@Override
	public String toString()
	{
		return this.body.toString();
	}
}
