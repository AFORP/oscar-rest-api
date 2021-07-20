/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api;

import com.cfa_afti.oscar_api.auth.BasicAuth;
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
import java.util.HashSet;
import java.util.Set;

public class HttpVerticle
	extends AbstractVerticle
{
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
	configureJWTAuth();

	BasicAuthHandler basicAuthHandler = BasicAuthHandler.create(SqlDbVerticle.sqlProvider);//Authorization: Basic <base64(email:pwd)>
	JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtProvider);//Authorization: Bearer <JWT>

	Router router = Router.router(this.vertx);

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
	/*
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

	router.get("/basic-auth/")
		.handler(basicAuthHandler)
		.handler(new BasicAuth(this.vertx, this.jwtProvider));

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
	  try
	  {
		rc.response().putHeader("Content-Type", "application/json");

		JsonResponse jr = new JsonResponse();

		rc.request().params().names().forEach((t) ->
		{
		  System.out.println(t);
		});

		rc.request().formAttributes().names().forEach((t) ->
		{
		  System.out.println(t);
		});

		/*
		 * Path >>> /path-params/12/
		 * URI >>> /path-params/12/?test=23
		 * Absolute URI >>> http://localhost:25000/path-params/12/?test=23
		 * Query >>> test=23
		 */
		jr.getResult()
			.put("Paramètre obligatoire", rc.request().getParam("userId"))
			.put("Paramètre optionnel", rc.request().getFormAttribute("inputName"));

		rc.response().end(jr.toString());
	  }
	  catch (Exception e)
	  {
		e.printStackTrace();
	  }

	});

	router.route("/exemple-requete").handler((routingContext) ->
	{
	  routingContext.response().putHeader("Content-Type", "application/json");

	  this.vertx.eventBus().request(
		  "sql_queries",
		  new JsonObject()
			  .put("query", "SELECT * FROM Users;"))
		  .onSuccess((msg) ->
		  {
			JsonArray ja = (JsonArray) msg.body();

			routingContext.response().end(ja.toString());
		  })
		  .onFailure((event) ->
			  routingContext.response().end(event.getMessage()));
	});

	router.route("/generate-pwd/:pwd/").handler((rc) ->
	{
	  rc.response().putHeader("Content-Type", "application/json");

	  String pwd = rc.request().getParam("pwd");
	  String hash;
	  boolean failurePwdGen = true;
	  JsonResponse jr = new JsonResponse();

	  try
	  {
		hash = SqlDbVerticle.sqlProvider.hash(
			"pbkdf2", // hashing algorithm (OWASP recommended)
			VertxContextPRNG.current().nextString(32), // secure random salt
			pwd // password
		);

		failurePwdGen = true;
	  }
	  catch (Exception e)
	  {
		e.printStackTrace();
	  }
	  finally
	  {

	  }

//			JsonObject jo = new JsonResponse()
//				.setHttpCode((short) 200)
//				.setResult(new JsonObject()
//					.put("pwd", pwd)
//					.put("hash", hash))
//				.getBody();
//
//			rc.response().end(jo.toString());
	});

	//router.route("/*").handler(jwtAuthHandler);
	/*
	 * GET /groupes/ - FAIL
	 * GET /scenarios/ - FAIL
	 * GET /scenarios/?groupe=n - FAIL
	 * GET /scenarios/:id/ - FAIL
	 * POST /scenarios/ :data - FAIL
	 *
	 * GET /groupes/ - FAIL
	 * POST /groupes/ :data - FAIL
	 *
	 */
	router.get("/scenarios/").handler((rc) ->
		new ScenarioController(vertx).handle(rc));
	router.get("/scenarios/:id/").handler((rc) ->
		new ScenarioController(vertx).handle(rc));

	router.get("/groupes/").handler((rc) ->
		new GroupeController(vertx).handle(rc));
	router.post("/groupes/:label/").handler((rc) ->
		new GroupeController(vertx).handle(rc));

	//router.put("/scenario/:data").handler(new ScenarioController(vertx));
//	router.get("/localisation/").handler((rc) ->
//		new LocalisationController(vertx));
	router.get("/etapes/").handler((rc) ->
		new EtapeController(this.vertx));
	router.post("/etapes/:data/").handler((rc) ->
		new EtapeController(this.vertx));

	//router.route().handler(chainAuthHandler);//<- A partir d'ici, tout nécéssite une authentification JWT.
//		router.route("/refresh").handler((rc) ->
//			new RefreshToken(this.jwtProvider, rc).start());
//		router.route("/jwt-auth").handler((routingContext) ->
//		{
//			HttpServerResponse response = routingContext.response();
//			response.putHeader("content-type", "text/plain");
//			response.write("Authentification JWT.");
//			response.end();
//		});
	//SSLCertificateFile /etc/letsencrypt/live/my-domain/cert9.pem
	//SSLCertificateKeyFile /etc/letsencrypt/live/my-domain/privkey9.pem
	vertx.createHttpServer(
		new HttpServerOptions()
			.setSsl(false))
		/*
		 * .setSsl(true)
		 * .setClientAuth(ClientAuth.REQUEST)
		 * .setPemKeyCertOptions(new PemKeyCertOptions()
		 * .setCertPath("LetsEncrypt/cert.pem")
		 * .setKeyPath("LetsEncrypt/privkey.pem"))
		 */
		.requestHandler(router)
		.listen(25000)
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
				  .setBuffer("-----BEGIN PUBLIC KEY-----\n"
					  + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2wOjnWcsbheZd8a6UzUi\n"
					  + "5PTEaquTxEO08+W0kc/TV9hOJxogQjTVB03+3KKjI1CYWi0YbdhHBJsnZsMIv99n\n"
					  + "qy0nWR+odsCK+SPI5YNMy0bzxY2O8f64pc6evyBKmvbu9Ixr+lHRwxlP7tmrsj/m\n"
					  + "/pa0n7yo12AmOTxnPKZsI9zYVh1vrfq9GbIyYe8h8Nm5pBv3SzGCQlyHUjdnGxZq\n"
					  + "r+vhIXEnvdtAD+6F8PApvNg88CI20h5bJmVwDftILnmkz6/c9VPvDiJofGFzcDiQ\n"
					  + "PPamt3nnfPUa/PNLYawYL4H0r0+MkADYoK2ehKsWNBh2Hj42BR3LxjRKoyUZ9by3\n"
					  + "uwIDAQAB\n"
					  + "-----END PUBLIC KEY-----"))
			  .addPubSecKey(new PubSecKeyOptions()
				  .setAlgorithm("RS256")
				  .setBuffer("-----BEGIN PRIVATE KEY-----\n"
					  + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDbA6OdZyxuF5l3\n"
					  + "xrpTNSLk9MRqq5PEQ7Tz5bSRz9NX2E4nGiBCNNUHTf7coqMjUJhaLRht2EcEmydm\n"
					  + "wwi/32erLSdZH6h2wIr5I8jlg0zLRvPFjY7x/rilzp6/IEqa9u70jGv6UdHDGU/u\n"
					  + "2auyP+b+lrSfvKjXYCY5PGc8pmwj3NhWHW+t+r0ZsjJh7yHw2bmkG/dLMYJCXIdS\n"
					  + "N2cbFmqv6+EhcSe920AP7oXw8Cm82DzwIjbSHlsmZXAN+0gueaTPr9z1U+8OImh8\n"
					  + "YXNwOJA89qa3eed89Rr880thrBgvgfSvT4yQANigrZ6EqxY0GHYePjYFHcvGNEqj\n"
					  + "JRn1vLe7AgMBAAECggEAVrGsFGSAy9t/nlAF9WX1OBhDn83nIiuC94CX55gSmpU+\n"
					  + "6m+HEW4EXW3cUs32McZ3aEqtft27zvDzudO+JOV0DehDyR2k+8zfthsaLO+6eETP\n"
					  + "vgV47gXcZZXSdOl9XrYchKUJIP8+PzJH185GDrsI3wIc4ZY2Z3rh5oooe3ONHuxT\n"
					  + "BYjia4lax/Kq96AnrQ6Sl2P3++pT4ubEvbjaRfVpTL1McRB4HFxH3aGCxv3FpL7m\n"
					  + "YZQcKlxSwSKPTIGkvw8fJOMAmM79P4rg36OltA1SOFaR97e5feZ0Xgzk5tGaz9H8\n"
					  + "6KfvdM25FWByyy2qOY+aQaz+CkMUMPj/EGfURQSfwQKBgQD1/hfDc63G+kICKWw/\n"
					  + "ocLEjd21uG/YZX1Fy1EJrDT/CpQofzD8ovyNGXCIXJgINInFqfWdS2lgaBAX1Jq/\n"
					  + "3kZFy1D8oL5/AfsDAHCPZ1ipIDVeBackj9a3eorR+FoYP11Kh3RUvLmgrDMbqSkf\n"
					  + "XkN4ERzruzvczDYM+O7aONR6iwKBgQDj7JQXMcvnAvvfhBchae27TJx7y+35Ou3T\n"
					  + "LR+PmEpdq8pgr2LVYcJL12TSm8eJTQ17l3adDepvJ1twWeSssz0oi3HUCVpYtsDm\n"
					  + "yp/mOnZAYhS55j77ylL2srDCUfyp2pVP/mbU8GCt96klfRCVuG0zZ92JxJydcfpz\n"
					  + "feUgQAbNkQKBgHDBP3M/mvAR1h/XjN697uDZhj69g8bU/k73mvWsEb61wqOtaW7j\n"
					  + "5o9mkcZvauCX9G6+MO8gmfSuvnGt6iD4aY2kXELwC2F8Lup5UR7qaCFduhiWzr5b\n"
					  + "kDgZ23fTNrjWkpSO8ivFNfEH/YU4TLksJBDtByymbhIPKNdZY2JzmjFxAoGAT47Y\n"
					  + "8m+zWOO1v4N//23WSbaoOJ4qZwCm2zu49IqYCrJYZf9SZGFHTOnWi51MvSRkPGvW\n"
					  + "P2QIHNyEEmeOZqn6AxkJlpXdL3I7S1QXFGn7tOWHjoxMF9+7rdkZ0fqEU1W1IMMQ\n"
					  + "aeuuE4uLQ0awb1J866Cpt9klQd/zKDUdsXAn78ECgYEAg2/QlKWHB2xCEq5vYtY7\n"
					  + "+PcFVmgLB259dconRLETe/5pV4RH7hwJQhvgKrjyzTUTPt/bQA86GvMmiyiNBJI/\n"
					  + "gNii8LTdTtc2KUAP3tca+qPZ0ezgA8l+NLDeiQzbu/TnLnsbAOB4RSp8OZ11Vpp7\n"
					  + "KDaRaEfgcf+z9PeM6yMiZCA=\n"
					  + "-----END PRIVATE KEY-----")
			  ));
	}
	catch (Exception e)
	{
	  e.printStackTrace();
	}
  }

//	private void configureWebClient()
//	{
//		WebClientOptions options = new WebClientOptions()
//			.setConnectTimeout(10000);
//		this.webClient = WebClient.create(this.vertx, options);
//	}
}
