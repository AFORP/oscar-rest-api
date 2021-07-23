/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.v1;

import com.cfa_afti.oscar_api.endpoints_id.EndpointsEtape;
import com.cfa_afti.oscar_api.tools.JsonResponse;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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
	private final Vertx vertx;
	private final EndpointsEtape etape;
	private RoutingContext rc;
	private JsonResponse jr;

	public EtapeController(Vertx vertx, EndpointsEtape etape)
	{
		this.vertx = vertx;
		this.etape = etape;
	}

	@Override
	public void handle(RoutingContext rc)
	{
		rc.response().putHeader("Content-Type", "application/json");

		this.rc = rc;
		this.jr = new JsonResponse();

		switch (etape)
		{
			case GET_ETAPE_BY_ID ->
				getEtapeById();
			case POST_ETAPE ->
				postEtape();
			case DELETE_ETAPE ->
				deleteEtape();
		}
	}

	private void getEtapeById()
	{
		String id = rc.request().getParam("id");

		this.vertx.eventBus().request(
			"sql_queries",
			new JsonObject()
				.put("query", "SELECT * FROM Etape WHERE id_etape = ?;")
				.put("query_params", new JsonArray().add(id)))
			.onSuccess((msg) ->
			{
				this.jr.setHttpCode(this.rc.response().getStatusCode());
				this.jr.setStatus("SUCCESS");

				System.out.println(msg.body().toString());

				if (((JsonObject) msg.body()).getJsonArray("data").size() == 0)
				{
					this.jr.getMessages().add("Aucune donnée correspondante.");
				}
				else
				{
					this.jr.getMessages().add("Acquisition des étapes réussie.");
					this.jr.setResult(new JsonObject().put("data", (JsonObject) msg.body()));
				}

				this.rc.response().setStatusCode(this.rc.response().getStatusCode());
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
	{
		JsonObject etapeData = new JsonObject(rc.request().getFormAttribute("data"));

		this.vertx.eventBus().request(
			"sql_queries",
			new JsonObject()
				.put("query", "INSERT INTO Etape VALUES (?, ?, ?, ?, ?, ?);")
				.put("query_params", new JsonArray()
					.add(etapeData.getInteger("id"))
					.add(etapeData.getString("description"))
					.add(etapeData.getString("type"))
					.add(etapeData.getString("validation"))
					.add(etapeData.getString("date_validation"))
					.add(etapeData.getInteger("id_scenario"))))
			.onSuccess((msg) ->
			{
				this.jr.setStatus("SUCCESS");
				this.jr.getMessages().add("Acquisition des étapes réussie.");
				this.jr.getResult().put("results", (JsonObject) msg.body());
			})
			.onFailure((event) ->
			{
				this.jr.setStatus("FAILED");
				this.jr.getMessages().add("Acquisition des étapes echouée.");
			});

	}

	private void deleteEtape()
	{
		int idScenario = Integer.parseInt(this.rc.request().getParam("id"));
		this.vertx.eventBus().request(
			"sql_queries",
			new JsonObject()
				.put("query", "DELETE FROM Etape WHERE id_etape=?")
				.put("query_params", new JsonArray()
					.add(idScenario)))
			.onSuccess((msg) ->
			{
				this.jr.setStatus("SUCCESS");
				this.jr.getMessages().add("Suppression de l'étape réussie.");
				this.jr.setResult(new JsonObject().put("data", (JsonObject) msg.body()));
				this.rc.response().end(this.jr.toString());
			})
			.onFailure((event) ->
			{
				this.jr.setStatus("FAILED");
				this.jr.getMessages().add("Suppression de l'étape echouée.");
				this.rc.response().end(this.jr.toString());
			});
	}
}
