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

/**
 *
 * @author Axel DUMAS <axel.dumas-ext@kct-france.com>
 */
public class EtapeController
	implements Handler<RoutingContext>
{
  private Vertx vertx;
  private RoutingContext rc;
  private JsonResponse jr;

  public EtapeController(Vertx vertx)
  {
	this.vertx = vertx;
  }

  @Override
  public void handle(RoutingContext rc)
  {
	rc.response().putHeader("Content-Type", "application/json");

	this.rc = rc;
	this.jr = new JsonResponse();

	if (rc.request().method() == HttpMethod.GET)
	{
//			if ()
//				rc.request().getParam("id");
//			getEtape();
	}
	else if (rc.request().method() == HttpMethod.POST)
	{
	  //putEtape();
	}
	else if (rc.request().method() == HttpMethod.DELETE)
	{
	  deleteEtape();
	}
	else
	{
	  System.out.println("Unsupported HTTP method.");
	}
  }

  private void getEtape(String id)
  {
	this.vertx.eventBus().request(
		"sql_queries",
		new JsonObject()
			.put("query", "SELECT * FROM Etape WHERE id_etape = ?;")
			.put("query_params", new JsonArray().add(12)))
		.onSuccess((msg) ->
		{
		  this.rc.response().setStatusCode(this.rc.response().getStatusCode());

		  this.jr.setHttpCode(this.rc.response().getStatusCode());
		  this.jr.setStatus("SUCCESS");

		  if (((JsonArray) msg.body()).toString().equals("[]"))
		  {
			this.jr.getMessages().add("Aucune donnée correspondante.");
		  }
		  else
		  {
			this.jr.getMessages().add("Acquisition des étapes réussie.");
			this.jr.setResult(new JsonObject().put("data", (JsonArray) msg.body()));
		  }

		  this.rc.response().end(jr.toString());
		})
		.onFailure((event) ->
		{
		  this.jr.setStatus("FAILED");
		  this.jr.getMessages().add("Acquisition des étapes echouée.");

		  this.rc.response().end(jr.toString());
		});
  }

  private void postEtape()
  {/*
	 * //:idScenario/:typeEtape/:descEtape
	 * String descEtape = rc.request().get;
	 * String typeEtape = "";
	 * int idScenario = 0;
	 *
	 * if (descEtape != null && typeEtape != null && idScenario != null)
	 * {
	 *
	 * }
	 *
	 * JsonObject etapeData = new JsonObject(this.rc.request().getParam("etapeData"));
	 * this.vertx.eventBus().request(
	 * "sql_queries",
	 * new JsonObject()
	 * .put("query", "INSERT INTO Etape VALUES (?, ?, ?, ?, ?, ?);")
	 * .put("query_params", new JsonArray()
	 * .add(etapeData.getInteger("id_etape"))
	 * .add(etapeData.getString("description_etape"))
	 * .add(etapeData.getString("type_etape"))
	 * .add(etapeData.getString("Validation"))
	 * .add(etapeData.getString("Date_Validation"))
	 * .add(etapeData.getInteger("id_scenario")))
	 * .onSuccess((msg) ->
	 * {
	 * this.jr.setStatus("SUCCESS");
	 * this.jr.getMessages().add("Acquisition des étapes réussie.");
	 * this.jr.getResult().put("results", (JsonArray) msg.body());
	 * })
	 * .onFailure((event) ->
	 * {
	 * this.jr.setStatus("FAILED");
	 * this.jr.getMessages().add("Acquisition des étapes echouée.");
	 * });
	 */

  }

  private void deleteEtape()
  {
	JsonObject idScenario = new JsonObject(this.rc.request().getParam("idScenario"));
	this.vertx.eventBus().request(
		"sql_queries",
		new JsonObject()
			.put("query", "DELETE FROM Scenario WHERE id_scenario=?")
			.put("query_params", new JsonArray()
				.add(idScenario.getString("idScenario"))))
		.onSuccess((msg) ->
		{
		  this.jr.setStatus("SUCCESS");
		  this.jr.getMessages().add("suppression du scénario réussie.");
		  this.jr.setResult(new JsonObject().put("data", (JsonArray) msg.body()));
		})
		.onFailure((event) ->
		{
		  this.jr.setStatus("FAILED");
		  this.jr.getMessages().add("suppression du scénario echouée.");
		});
  }
}
