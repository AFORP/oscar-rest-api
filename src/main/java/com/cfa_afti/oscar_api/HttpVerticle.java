/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api;

import static com.cfa_afti.oscar_api.MainVerticle.API_CONFIG;
import com.cfa_afti.oscar_api.auth.BasicAuth;
import com.cfa_afti.oscar_api.endpoints_id.EndpointsEtape;
import com.cfa_afti.oscar_api.endpoints_id.EndpointsGroupe;
import com.cfa_afti.oscar_api.endpoints_id.EndpointsScenario;
import com.cfa_afti.oscar_api.tools.JsonResponse;
import com.cfa_afti.oscar_api.v1.EtapeController;
import com.cfa_afti.oscar_api.v1.GroupeController;
import com.cfa_afti.oscar_api.v1.ScenarioController;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpVerticle
	extends AbstractVerticle
{
	public static final Logger LOGGER = Logger.getLogger(HttpVerticle.class.getName());
	public static HttpVerticle verticleInstance = null;

	private HttpServer httpServer;
	public JWTAuth jwtProvider;

	public HttpVerticle()
	{
	}

	@Override
	public void start(Promise<Void> startPromise)
		throws Exception
	{
		configureJWTAuth();//Permet à un utilisateur de se connecter à l'API via un Json Web Token (JWT).

		BasicAuthHandler basicAuthHandler = BasicAuthHandler.create(SqlDbVerticle.sqlProvider);//Authorization: Basic <base64(email:pwd)>
		JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtProvider);//Authorization: Bearer <JWT>

		Router router = Router.router(this.vertx);

		//En-têtes autorisées. Selon vos besoins.
		Set<String> allowedHeaders = new HashSet<>();
		allowedHeaders.add(HttpHeaders.ORIGIN.toString());
		allowedHeaders.add(HttpHeaders.CONTENT_TYPE.toString());
		allowedHeaders.add(HttpHeaders.AUTHORIZATION.toString());
		allowedHeaders.add(HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());
		allowedHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString());
		allowedHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS.toString());
		allowedHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS.toString());
		allowedHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.toString());
		allowedHeaders.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS.toString());
		allowedHeaders.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD.toString());
		allowedHeaders.add(HttpHeaders.VARY.toString());

		//Méthodes HTTP autorisées. Selon vos besoins.
		Set<HttpMethod> allowedMethods = new HashSet<>();
		allowedMethods.add(HttpMethod.GET);
		allowedMethods.add(HttpMethod.POST);
		allowedMethods.add(HttpMethod.PUT);
		allowedMethods.add(HttpMethod.PATCH);
		allowedMethods.add(HttpMethod.DELETE);
		allowedMethods.add(HttpMethod.OPTIONS);

		router.route().handler(CorsHandler.create()
			.addOrigin("http://localhost:25000/")
			.allowCredentials(true)
			.allowedHeaders(allowedHeaders)
			.allowedMethods(allowedMethods));

		router.route().handler(BodyHandler.create());

		/* [===HELP===]
		 * Options autorisées dans les API REST :
		 * GET : Accède à une ressource.
		 * POST : Ajoute une ressource.
		 * PUT : Met à jour une ressource omplète en la remplacent par une
		 * nouvelle version (99% des cas).
		 * PATCH : Met à jour une partie d'une ressource en envoyant un
		 * différentiel (sorte de git diff).
		 * DELETE : Supprime une ressource.
		 */
		router.route("/").handler((rc) ->
			rc.response().end("Bienvenue sur l'API du projet OSCAR !"));

		router.post("/basic-auth/")
			.handler(basicAuthHandler)
			.handler(new BasicAuth(this.vertx, this.jwtProvider));

		/* [===HELP===]
		 * Lorsque vous executez l'API, dans votre navigateur, vous pouvez écrire :
		 *		localhost:[port]/examples/
		 * afin d'obtenir une liste des requêtes possibles.
		 */
		router.get("/examples/")
			.handler((rc) ->
			{
				rc.response().putHeader("Content-Type", "application/json");//L'en-tête pour avertir les navigateurs que l'on envoie du JSON.
				JsonResponse jr = new JsonResponse();//La réponse JSON qui sera faite aux utilisateurs.
				jr.getResult()
					.put("/examples/", "Cette page. Vous y trouverez tous les exemples qui pourront vous aider.")
					.put("/", "Page d'accueil de l'API.")
					.put("/path-params/:userId/?userName", "Pour tester la réception de paramètres dans l'URL.")
					.put("/exemple-requete", "Un exemple de requête SQL.")
					.put("/generate-pwd", "Pour générer un mot de passe.");

				rc.response().end(jr.toString());//On envoie la réponse au client.
			});

		router.post("/path-params/:userId/").handler((rc) ->
		{
			/*
			 * Le try/catch n'est pas obligatoire.
			 * Mais si vous avez une erreur que vous ne comprenez pas, essayez de l'inclure.
			 */
			try
			{
				rc.response().putHeader("Content-Type", "application/json"); //Le document que l'on renvoit est un JSON.

				/*
				 * La classe JsonResponse vous aide à créer la réponse à une requête.
				 */
				JsonResponse jr = new JsonResponse();

				rc.request().params().names().forEach((t) ->
				{
					System.out.println(t);
				});

				rc.request().formAttributes().names().forEach((t) ->
				{
					System.out.println(t);
				});

				/* [===HELP===]
				 * Voici un exemple de ce que vous pouvez trouver en faisant rc.request().*
				 * .path() >>> /path-params/12/
				 * .uri() >>> /path-params/12/?test=23
				 * .absoluteURI() >>> http://localhost:25000/path-params/12/?test=23
				 * .query() >>> test=23
				 */
				jr.getResult()
					.put("Paramètre obligatoire", rc.request().getParam("userId")) //superURL/:userId
					.put("Paramètre optionnel", rc.request().getFormAttribute("inputName")); //superURL?inputName=Toto

				rc.response().end(jr.toString());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

		});

		/* [===HELP===]
		 * Pour celles et ceux qui reprendront ce code, voici un petit
		 * exemple de ce à quoi ressemble la gestion d'une requête.
		 */
		router.route("/exemple-requete").handler((routingContext) ->
		{
			routingContext.response().putHeader("Content-Type", "application/json");

			this.vertx.eventBus().request(
				"sql_queries",
				new JsonObject()
					.put("query", "SELECT * FROM Users;"))
				.onSuccess((msg) ->
				{
					/*
					 * msg est un objet "Message" circulant à travers le bus d'événements.
					 *
					 */
					JsonArray ja = (JsonArray) msg.body();

					routingContext.response().end(ja.toString());
				})
				.onFailure((event) ->
					routingContext.response().end(event.getMessage()));
		});


		router.get("/generate-pwd/:pwd").handler((rc) ->
		{
			rc.response().putHeader("Content-Type", "application/json");

			String pwd = rc.request().getParam("pwd");
			String hash;

			hash = SqlDbVerticle.sqlProvider.hash(
				"pbkdf2", //Algorithme de Hashage (recommandé par l'OWASP)
				VertxContextPRNG.current().nextString(32), //Sel sécurisé et aléatoire
				pwd); //Mot de passe à hacher

			JsonResponse jr = new JsonResponse();

			jr.setStatus("SUCCESS");
			jr.getResult()
				.put("pwd", pwd)
				.put("pwd-hash", hash);

			rc.response().end(jr.toString());
		});

		//Toutes les routes

		router.get("/scenarios/").handler((rc) ->
			new ScenarioController(vertx, EndpointsScenario.GET_SCENARIOS).handle(rc));
		router.get("/scenarios/:id/").handler((rc) ->
			new ScenarioController(vertx, EndpointsScenario.GET_SCENARIO).handle(rc));
		router.post("/scenarios/").handler((rc) ->
			new ScenarioController(vertx, EndpointsScenario.POST_SCENARIO).handle(rc));
		router.delete("/scenarios/:id/").handler((rc) ->
			new ScenarioController(vertx, EndpointsScenario.DELETE_SCENARIO).handle(rc));

		router.get("/groupes/").handler((rc) ->
			new GroupeController(vertx, EndpointsGroupe.GET_GROUPES).handle(rc));
		router.post("/groupes/:label/").handler((rc) ->
			new GroupeController(vertx, EndpointsGroupe.POST_GROUPE).handle(rc));
		router.delete("/groupes/:id/").handler((rc) ->
			new GroupeController(vertx, EndpointsGroupe.DELETE_GROUPE).handle(rc));

		router.get("/etapes/:id/").handler((rc) ->
			new EtapeController(this.vertx, EndpointsEtape.GET_ETAPE_BY_ID).handle(rc));
		router.post("/etapes/").handler((rc) ->
			new EtapeController(this.vertx, EndpointsEtape.POST_ETAPE).handle(rc));
		router.delete("/etapes/:id/").handler((rc) ->
			new EtapeController(this.vertx, EndpointsEtape.DELETE_ETAPE).handle(rc));

//		router.route().handler(chainAuthHandler);//<- A partir d'ici, tout nécéssite une authentification JWT.
//		router.route("/refresh").handler((rc) ->
//			new RefreshToken(this.jwtProvider, rc).start());
//		router.route("/jwt-auth").handler((routingContext) ->
//		{
//			HttpServerResponse response = routingContext.response();
//			response.putHeader("content-type", "text/plain");
//			response.write("Authentification JWT.");
//			response.end();
//		});

		vertx.createHttpServer(
			new HttpServerOptions())
//				.setSsl(true)
//				.setClientAuth(ClientAuth.REQUEST)
//				.setPemKeyCertOptions(new PemKeyCertOptions()
//					.setCertPath("/path/to/cert.pem")
//					.setKeyPath("/path/to/privkey.pem")))
			.requestHandler(router)
			.listen(API_CONFIG.getInteger("http.port"))
			.onSuccess((server) ->
			{
				System.out.println("Serveur démarré sur le port : "
					+ server.actualPort() + ".");
			})
			.onFailure((event) ->
			{
				System.out.println("Echec de démarrage du serveur : "
					+ event.getMessage());
			});

		startPromise.complete();
	}

	public HttpServer getHttpServer()
	{
		return httpServer;
	}

	private void configureJWTAuth()
	{
		try
		{
			this.jwtProvider = JWTAuth.create(
				this.vertx,
				new JWTAuthOptions()
					.addPubSecKey(new PubSecKeyOptions()
						.setAlgorithm("RS256")
						.setBuffer(Files.readString(Path.of(new URI("file:///opt/oscar-rest-api/keys/publicKey.pem")))))
					.addPubSecKey(new PubSecKeyOptions()
						.setAlgorithm("RS256")
						.setBuffer(Files.readString(Path.of(new URI("file:///opt/oscar-rest-api/keys/privateKey.pem"))))
					));
		}
		catch (IOException | URISyntaxException e)
		{
			e.printStackTrace();

			LOGGER.log(Level.SEVERE, "Fichier introuvable : {0}.", e.getMessage());
			this.vertx.close();
		}
	}
}
