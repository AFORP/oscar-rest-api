/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

/**
 * Lancement : java -jar 'OSCAR_API-1.0-SNAPSHOT.jar' run com.cfa_afti.oscar_api.MainVerticle
 * @author Axel DUMAS <axel.dumas-ext@kct-france.com>
 */
public class MainVerticle extends AbstractVerticle
{
	public MainVerticle()
	{
	}

	 @Override
	public void start(Promise<Void> promise)
	{
		vertx.deployVerticle(new SqlDbVerticle())
			.onSuccess((event) ->
			{
				System.out.println("SqlDbVerticle déployé.");
				this.vertx.deployVerticle(new HttpVerticle())
					.onSuccess((e) ->
					{
						System.out.println("HttpVerticle déployé.");

					})
					.onFailure((e) ->
					{
						System.out.println("HttpVerticle non déployé : "
							+ e.getMessage());
					});
			})
			.onFailure((event) ->
			{
				System.out.println("SqlDbVerticle non déployé : "
					+ event.getMessage());
			});
	}
}
