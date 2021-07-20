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
 * @author lucas
 */
public class LocalisationController
	implements Handler<RoutingContext>
{

	private Vertx vertx;
	private RoutingContext rc;
	private JsonResponse jr;

	public LocalisationController(Vertx vertx)
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

		}
		else if (rc.request().method() == HttpMethod.POST)
		{
			//postGroupe();
		}
		else if (rc.request().method() == HttpMethod.DELETE)
		{

		}
		else
		{
			System.out.println("Unsupported HTTP method.");
		}

		rc.response().end(jr.toString());
	}

	public void returnGroupe()
	{
		try
		{
			rc.response().putHeader("Content-Type", "application/json");

			this.vertx.eventBus().request(
				"sql_queries",
				new JsonObject()
					.put("query", "SELECT * FROM Localisation;"))
				.onSuccess((msg) ->
				{
					this.jr.setStatus("SUCCESS");
					this.jr.getMessages().add("return des localisation réussie.");
					this.jr.getResult().put("data", (JsonArray) msg.body());//this.jr.setResult(new JsonObject().put("data", (JsonArray) msg.body()));
				})
				.onFailure((event) ->
				{
					this.jr.setStatus("FAILED");
					this.jr.getMessages().add("return des localisations echouée.");
				});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
