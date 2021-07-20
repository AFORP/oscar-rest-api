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

public class UserController
	implements
	Handler<RoutingContext>
        
        
{
        private Vertx vertx;
	private RoutingContext rc;
	private JsonResponse jr;
        
	@Override
	public void handle(RoutingContext rc)
	{
		switch (rc.request()
			.method().name())
		{
			case "GET":

			case "POST":

			case "DELETE":

			default:
				System.err.println("HTTP method not handled here.");
		}

	}
        
        public void getUser(){
            rc.response().putHeader("Content-Type", "application/json");

		this.vertx.eventBus().request(
			"sql_queries",
			new JsonObject()
				.put("query", "SELECT * FROM Users;"))
			.onSuccess((msg) ->
			{
				this.jr.setStatus("SUCCESS");
				this.jr.getMessages().add("return des users réussie.");
                                this.jr.setResult(new JsonObject().put("data", (JsonArray) msg.body()));
			})
			.onFailure((event) ->
			{
				this.jr.setStatus("FAILED");
				this.jr.getMessages().add("return des groupes echouée.");
			});
        }

}
