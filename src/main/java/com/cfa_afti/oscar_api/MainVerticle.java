/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.util.logging.Logger;

/**
 * @author Axel DUMAS <axel.dumas-ext@kct-france.com>
 */
public class MainVerticle extends AbstractVerticle
{
	private static final Logger LOGGER = Logger.getLogger(MainVerticle.class.getName());
	public static JsonObject API_CONFIG;

	public MainVerticle()
	{
	}

	@Override
	public void start(Promise<Void> promise)
	{
		/* [===HELP===]
		 * Bonjour ! Je vais essayer de vous guider pour la reprise de ce code.
		 * Toutes les plus grosses aides sont marquées dans des commentaires multilignes.
		 * Vous pourrez les retrouver dans le code en recherchant la chaîne suivante : [===HELP===]
		 *
		 * Vous remarquerez qu'il n'y a pas de classe Main. C'est normal.
		 * Au lieu de ça, l'application commence...Ici. Dans le MainVerticle.
		 * La classe Main est affichée dans le fichier pom.xml : io.vertx.core.Launcher.
		 * Elle nous permet d'accéder à quelques fonctionnalités supplémentaires.
		 * Le lancement est donc un peu différent de d'habitude :
		 *		java -jar 'OSCAR_API-1.0-SNAPSHOT.jar' run com.cfa_afti.oscar_api.MainVerticle
		 *
		 * Un verticle peut être vu comme une grosse brique.
		 * Il est préférable qu'elle ne remplisse qu'une seule tâche.
		 *
		 * Ici, nous avons SqlDbVerticle qui se charge de la base de données et des requêtes.
		 * Nous avons aussi HttpVerticle qui se charge de la réception des requêtes et des réponses.
		 *
		 * Voici quelques liens pour vous aider :
		 * https://vertx.io/introduction-to-vertx-and-reactive/ - Introduction à Vertx + Systèmes asynchrones.
		 * https://vertx.io/docs/vertx-core/java/ - Les fonctionnalités de base de Vertx.
		 * https://vertx.io/docs/vertx-web/java/ - La partie Web.
		 * https://vertx.io/docs/vertx-mysql-client/java/ - La partie SQL.
		 */

		/*
		 * Le fichier de configuration à chercher.
		 * Voir : https://vertx.io/docs/vertx-config/java/
		 */
		ConfigStoreOptions fileStore = new ConfigStoreOptions()
			.setType("file")
			.setConfig(new JsonObject().put("path", "/opt/oscar-rest-api/conf/api-conf.json"));//"/opt/oscar_api/conf/api-conf.json"

		ConfigRetriever retriever = ConfigRetriever.create(
			vertx,
			new ConfigRetrieverOptions()
				.addStore(fileStore));

		retriever.getConfig((config) ->
		{
			API_CONFIG = config.result();//Variable statique afin d'obtenir les données du fichier de configuration depuis n'importe où.

			deploySqlDbVerticle().compose((verticleId) ->
			{
				return deployHttpVerticle();
			});
		});
	}

	private Future<String> deploySqlDbVerticle()
	{
		return this.vertx.deployVerticle(new SqlDbVerticle())
			.onSuccess((verticleId) ->
		{
			LOGGER.info(String.format("SqlDbVerticle déployé avec l'ID : %s.", verticleId));
		})
			.onFailure((event) ->
		{
			LOGGER.severe(String.format("SqlDbVerticle non déployé. Cause : %s.", event.getMessage()));
		});
	}

	private Future<String> deployHttpVerticle()
	{
		return this.vertx.deployVerticle(new HttpVerticle())
			.onSuccess((verticleId) ->
		{
			LOGGER.info(String.format("HttpVerticle déployé avec l'ID : %s.", verticleId));
		})
			.onFailure((event) ->
		{
			LOGGER.severe(String.format("HttpVerticle non déployé. Cause : %s.", event.getMessage()));
		});
	}

}
