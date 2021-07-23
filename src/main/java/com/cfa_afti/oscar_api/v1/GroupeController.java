/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.v1;

import com.cfa_afti.oscar_api.endpoints_id.EndpointsGroupe;
import com.cfa_afti.oscar_api.tools.JsonResponse;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroupeController
	implements Handler<RoutingContext>
{
	private final Vertx vertx;
	private final EndpointsGroupe groupe;
	private final JsonResponse jr;
	private RoutingContext rc;

	public GroupeController(Vertx vertx, EndpointsGroupe groupe)
	{
		this.vertx = vertx;
		this.groupe = groupe;
		this.jr = new JsonResponse();
	}

	@Override
	public void handle(RoutingContext rc)
	{
		this.rc = rc;
		rc.response().putHeader("Content-Type", "application/json");

		switch (groupe)
		{
			case GET_GROUPES ->
				getGroupes();
			case POST_GROUPE ->
				postGroupe();
				case DELETE_GROUPE ->
				deleteGroup();
			default ->
			{
				this.jr.setStatus("FAILED");
				this.jr.getMessages().add("HTTP method not recognized.");
				rc.response().end(this.jr.toString());
			}
		}
	}

	private void getGroupes()
	{
		this.vertx.eventBus().request(
			"sql_queries",
			new JsonObject().put("query", "SELECT id_groupe, label_groupe FROM Groupe;"))
			.onSuccess((msg) ->
			{
				JsonObject jo = (JsonObject) msg.body();
				this.jr.setStatus("SUCCESS");
				this.jr.setHttpCode(200);
				this.jr.getResult().put("result_query", jo);

				if (jo.size() == 0)
					this.jr.getMessages().add("No returned data.");



				this.rc.response().setStatusCode(200);
				this.rc.response().end(this.jr.toString());
			})
			.onFailure((event) ->
			{
				this.jr.setStatus("FAILED");
				this.jr.setHttpCode(500);
				this.jr.getMessages().add(event.getMessage());
				this.rc.response().end(this.jr.toString());
			});
	}

	private void deleteGroup()
	{
		int idGroupe = Integer.parseInt(this.rc.request().getParam("id"));
		this.vertx.eventBus().request(
			"sql_queries",
			new JsonObject()
				.put("query", "DELETE FROM Groupe WHERE id_groupe=?")
				.put("query_params", new JsonArray()
					.add(idGroupe)))
			.onSuccess((msg) ->
			{
				this.jr.setStatus("SUCCESS");
				this.jr.setHttpCode(200);
				this.jr.getMessages().add("Suppression du groupe réussie.");
				this.jr.setResult(new JsonObject().put("data", (JsonObject) msg.body()));
				this.rc.end(this.jr.toString());
			})
			.onFailure((event) ->
			{
				this.jr.setStatus("FAILED");
				this.jr.setHttpCode(500);
				this.jr.getMessages().add("Suppression du groupe echouée : " + event.getMessage());
				this.rc.response().end(this.jr.toString());
				this.rc.end(this.jr.toString());
			});


	}

	private void postGroupe()
	{
		String labelGroupe = rc.request().getParam("label");

		try
		{
			labelGroupe = URLDecoder.decode(labelGroupe, StandardCharsets.UTF_8.toString());
		}
		catch (UnsupportedEncodingException ex)
		{
			Logger.getLogger(GroupeController.class.getName()).log(Level.SEVERE, null, ex);
		}

		this.vertx.eventBus().request("sql_queries", new JsonObject()
			.put("query", "INSERT INTO Groupe (id_groupe, label_groupe) VALUES(?);")
			.put("query_params", new JsonArray().add(labelGroupe)))
			.onSuccess((msg) ->
			{
				this.jr.setStatus("SUCCESS");
				this.jr.getResult().put("result_query", (JsonObject) msg.body());
				this.rc.response().end(this.jr.toString());
			})
			.onFailure((event) ->
			{
				this.jr.setStatus("FAILURE");
				this.jr.getMessages().add(event.getMessage());
				this.rc.response().end(this.jr.toString());
			});
	}
}
