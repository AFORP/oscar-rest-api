/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.v1;

import com.cfa_afti.oscar_api.tools.JsonResponse;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
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
  private final JsonResponse jr;
  private RoutingContext rc;

  public GroupeController(Vertx vertx)
  {
	this.vertx = vertx;
	this.jr = new JsonResponse();
  }

  @Override
  public void handle(RoutingContext rc)
  {
	this.rc = rc;
	rc.response().putHeader("Content-Type", "application/json");

	try
	{
	  if (rc.request().method() == HttpMethod.GET)
	  {
		getGroupes();
	  }
	  else if (rc.request().method() == HttpMethod.POST)
	  {
		if (rc.request().getParam("label") != null)
		{
		  postGroupe(rc.request().getParam("label"));
		}
	  }
	  else if (rc.request().method() == HttpMethod.DELETE)
	  {
	  }
	  else
	  {
		this.jr.setStatus("FAILED");
		this.jr.getMessages().add("HTTP method not recognized.");
		rc.response().end(this.jr.toString());
	  }
	}
	catch (Exception e)
	{
	  e.printStackTrace();
	}

  }

  private void getGroupes()
  {
	this.vertx.eventBus().request(
		"sql_queries",
		new JsonObject().put("query", "SELECT id_groupe, label_groupe FROM Groupe;"))
		.onSuccess((msg) ->
		{
		  JsonArray ja = (JsonArray) msg.body();
		  this.jr.setStatus("SUCCESS");
		  this.jr.getResult().put("result_query", ja);

		  if (ja.size() == 0)
			this.jr.getMessages().add("No returned data.");

		  rc.response().end(this.jr.toString());
		})
		.onFailure((event) ->
		{
		  this.jr.setStatus("FAILED");
		  this.jr.getMessages().add(event.getMessage());
		});
  }

  private void deleteGroup()
  {
	JsonObject idGroupe = new JsonObject(this.rc.request().getParam("idGroupe"));
	this.vertx.eventBus().request(
		"sql_queries",
		new JsonObject()
			.put("query", "DELETE FROM Groupe WHERE id_groupe=?")
			.put("query_params", new JsonArray()
				.add(idGroupe.getString("idGroupe"))))
		.onSuccess((msg) ->
		{
		  this.jr.setStatus("SUCCESS");
		  this.jr.getMessages().add("suppression du groupe réussie.");
		  this.jr.setResult(new JsonObject().put("data", (JsonArray) msg.body()));
		})
		.onFailure((event) ->
		{
		  this.jr.setStatus("FAILED");
		  this.jr.getMessages().add("suppression du groupe echouée.");
		});

	this.rc.end(this.jr.toString());
  }

  private void postGroupe(String labelGroupe)
  {
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
