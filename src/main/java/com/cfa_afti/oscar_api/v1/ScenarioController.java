/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.v1;

import com.cfa_afti.oscar_api.endpoints_id.EndpointsScenario;
import com.cfa_afti.oscar_api.tools.JsonQueryResponse;
import com.cfa_afti.oscar_api.tools.JsonResponse;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ScenarioController
	implements Handler<RoutingContext>
{
	private final Vertx vertx;
	private final EndpointsScenario scenario;
	private final JsonResponse jr;
	private RoutingContext rc;

	public ScenarioController(Vertx vertx, EndpointsScenario scenario)
	{
		this.vertx = vertx;
		this.scenario = scenario;
		this.jr = new JsonResponse();
	}

	@Override
	public void handle(RoutingContext rc)
	{
		this.rc = rc;

		rc.response().putHeader("Content-Type", "application/json");

		switch (scenario)
		{
			case GET_SCENARIOS ->
				getScenarios();
			case GET_SCENARIOS_FROM_GROUP ->
				getScenariosFromGroup();
			case GET_SCENARIO ->
				getScenario();
			case POST_SCENARIO ->
				postScenario();
			case DELETE_SCENARIO ->
				deleteScenario();
		}
	}

	private void postScenario()
	{
		String data = this.rc.request().getFormAttribute("data");

		try
		{
			JsonObject scenarioData = new JsonObject(data);
			this.vertx.eventBus().request(
				"sql_queries",
				new JsonObject()
					.put("query", "INSERT INTO Scenario VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
					.put("query_params", new JsonArray()
						.add(scenarioData.getInteger("id"))
						.add(scenarioData.getString("titre"))
						.add(scenarioData.getString("description"))
						.add(scenarioData.getString("date_publication"))
						.add(scenarioData.getInteger("publie"))
						.add(scenarioData.getInteger("id_groupe"))
						.add(scenarioData.getInteger("id_user"))
						.add(scenarioData.getInteger("id_localisation"))))
				.onSuccess((msg) ->
				{
					this.jr.setStatus("SUCCESS");
					this.jr.setHttpCode(200);
					this.jr.getMessages().add("Insertion du scénario réussie.");
					this.jr.setResult(new JsonObject().put("data", (JsonObject) msg.body()));
					this.rc.response().setStatusCode(200);
					this.rc.end(this.jr.toString());
				})
				.onFailure((event) ->
				{
					this.jr.setStatus("FAILED");
					this.jr.setHttpCode(500);
					this.jr.getMessages().add("Insertion du scénario echouée.");
					this.rc.response().setStatusCode(500);
					this.rc.end(this.jr.toString());
				});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void deleteScenario()
	{
		int idScenario = Integer.parseInt(this.rc.request().getParam("id"));

		this.vertx.eventBus().request(
			"sql_queries",
			new JsonObject()
				.put("query", "DELETE FROM Scenario WHERE id_scenario=?;")
				.put("query_params", new JsonArray()
					.add(idScenario)))
			.onSuccess((msg) ->
			{
				this.jr.setStatus("SUCCESS");
					this.jr.setHttpCode(200);
					this.jr.getMessages().add("Suppression du scénario réussie.");
					this.rc.response().setStatusCode(200);
					this.rc.end(this.jr.toString());
			})
			.onFailure((event) ->
			{
				this.jr.setStatus("FAILED");
					this.jr.setHttpCode(500);
					this.jr.getMessages().add("Suppression du scénario échouée.");
					this.rc.response().setStatusCode(500);
					this.rc.end(this.jr.toString());
			});
	}

	private void getScenarios()
	{
		JsonObject jo = new JsonObject()
			.put("query", "SELECT * FROM Scenario;");

		this.vertx.eventBus().request("sql_queries", jo)
			.onSuccess((msg) ->
			{
				this.jr.setHttpCode(this.rc.response().getStatusCode());
				this.jr.setStatus("SUCCESS");
				System.out.println(msg.body());
				this.jr.getResult().put("sql_result", msg.body());

				if (((JsonObject) msg.body()).getJsonArray("data").size() == 0)
				{
					this.jr.getMessages().add("No returned data.");
				}

				this.rc.response().end(this.jr.toString());
			})
			.onFailure((event) ->
			{
				this.jr.setHttpCode(this.rc.response().getStatusCode());
				this.jr.setStatus("FAILED");
				this.jr.getMessages().add(event.getMessage());

				this.rc.response().end(this.jr.toString());
			});
	}

	private void getScenariosFromGroup()
	{
		String idGroup = rc.request().getParam("groupe");
		this.vertx.eventBus().request("sql_queries", new JsonObject()
			.put("query", "SELECT Scenario.id_scenario, Scenario.titre_scenario, Groupe.id_groupe, Groupe.label_groupe FROM Scenario INNER JOIN Groupe ON Scenario.id_groupe = Groupe.id_groupe WHERE Groupe.id_groupe=?;")
			.put("query_params", new JsonArray().add(idGroup)))
			.onSuccess((msg) ->
			{
				JsonObject jo = (JsonObject) msg.body();

				if (jo.getJsonArray("data").size() > 0)
				{
					this.jr.getResult().put("sql_result", jo);
				}

				this.jr.setStatus("SUCCESS");
				this.jr.setHttpCode(this.rc.statusCode());
				this.rc.response().end(this.jr.toString());
			})
			.onFailure(((event) ->
			{
				this.jr.setStatus("FAILED");
				this.rc.response().end(this.jr.toString());
			}));
	}

	private void getScenario()
	{
		getAllScenariosAndLocalization();
			//.compose(scenarioId -> getAllTasks(, requiredEtapes, etapesJson)teps(scenarioId));
//			.compose((t) ->
//			{
//				getAllTasks(conditionsSqlSB, requiredEtapes, etapesJson)
//				return null; //To change body of generated lambdas, choose Tools | Templates.
//			});
	}

	private Future<Integer> getAllScenariosAndLocalization()
	{
		Promise promise = Promise.promise();

		int scenarioId = Integer.parseInt(rc.request().getParam("id"));

		this.vertx.eventBus().request("sql_queries", new JsonObject()
			.put("query", """
      SELECT * FROM Scenario
      INNER JOIN Localisation ON Localisation.id_localisation = Scenario.id_localisation WHERE id_scenario=?;""")
			.put("query_params", new JsonArray().add(scenarioId)))
			.onSuccess((msgQueryScenario) ->
			{
				JsonQueryResponse scenariosJson = new JsonQueryResponse(msgQueryScenario.body());

				this.jr.setHttpCode(this.rc.response().getStatusCode());
				this.jr.setStatus("SUCCESS");
				this.jr.setResult(new JsonObject()
					.put("id_scenario", scenariosJson.getData().getJsonObject(0).getInteger("id_scenario"))
					.put("titre_scenario", scenariosJson.getData().getJsonObject(0).getString("titre_scenario"))
					.put("description_scenario", scenariosJson.getData().getJsonObject(0).getString("description_scenario"))
					.put("date_publication", scenariosJson.getData().getJsonObject(0).getString("date_publication"))
					.put("publie_scenario", scenariosJson.getData().getJsonObject(0).getString("publie_scenario"))
					.put("id_groupe", scenariosJson.getData().getJsonObject(0).getInteger("id_groupe"))
					.put("id_user", scenariosJson.getData().getJsonObject(0).getInteger("id_user"))
					.put("location", new JsonObject()
						.put("id_location", scenariosJson.getData().getJsonObject(0).getInteger("id_localisation"))
						.put("site", scenariosJson.getData().getJsonObject(0).getString("Site"))
						.put("batiment", scenariosJson.getData().getJsonObject(0).getString("Batiment"))
						.put("etage", scenariosJson.getData().getJsonObject(0).getString("Etage"))
						.put("salle_num", scenariosJson.getData().getJsonObject(0).getString("Salle_n"))
						.put("salle_nom", scenariosJson.getData().getJsonObject(0).getString("Salle_nom"))));

				//GetALL_STEPS
				getAllSteps(scenarioId);
				promise.complete(scenarioId);
			})
			.onFailure((event) ->
			{
				this.jr.setHttpCode(this.rc.response().getStatusCode());
				this.jr.setStatus("FAILED");
				this.jr.getMessages().add(event.getMessage());
				this.rc.response().end(this.jr.toString());
				promise.fail(event.getMessage());
			});

		return promise.future();
	}

	private Future<JsonObject> getAllSteps(int scenarioId)
	{
		Promise promise = Promise.promise();

		this.vertx.eventBus().request("sql_queries", new JsonObject()
			.put("query", "SELECT * FROM Etape WHERE id_scenario=? ORDER BY num_etape ASC;")
			.put("query_params", new JsonArray().add(scenarioId)))
			.onSuccess((msgQueryEtapes) ->
			{
				JsonQueryResponse etapesJson = new JsonQueryResponse(msgQueryEtapes.body());
				StringBuilder conditionsSqlSB = new StringBuilder(4);
				JsonArray requiredEtapes = new JsonArray();

				//Pour la requete de réception de tâches.
				for (int i = 0; i < etapesJson.getData().size(); i++)
				{
					JsonObject etape = etapesJson.getData().getJsonObject(i);
					if (i < etapesJson.size())
						conditionsSqlSB.append("id_etape=? or ");
					else
						conditionsSqlSB.append("id_etape=? ");

					requiredEtapes.add(etape.getInteger("id_etape"));
				}

				//Sélectionner les taches lorsqu'une etape en a.
				this.jr.getResult().put("etapes", etapesJson.getData());

				if (requiredEtapes.size() == 0)
				{
					this.jr.setStatus("SUCCESS");
					this.rc.response().end(this.jr.toString());
					System.out.println(this.jr.toString());
				}
				else
				{
					this.rc.response().end(this.jr.toString());
					//GET_ALL_TASKS
					getAllTasks(conditionsSqlSB, requiredEtapes, etapesJson);
				}

				promise.complete(new JsonObject()
				.put("sql-conditions", conditionsSqlSB)
				.put("etapes-extended", etapesJson)
				.put("etapes_id", requiredEtapes));
			})
			.onFailure((event) ->
			{
				this.jr.getMessages().add(event.getMessage());
				this.rc.response().end(this.jr.toString());
				promise.fail(event.getMessage());
			});

		return promise.future();
	}

	private Future<Void> getAllTasks(StringBuilder conditionsSqlSB, JsonArray requiredEtapes, JsonQueryResponse etapesJson)
	{
		Promise promise = Promise.promise();

		this.vertx.eventBus().request("sql_queries", new JsonObject()
			.put("query", "SELECT * FROM Tache WHERE " + conditionsSqlSB.toString()
				+ " ORDER BY ordre_tache ASC;")
			.put("query_params", requiredEtapes))
			.onSuccess((msgQueryTaches) ->
			{
				JsonQueryResponse tachesJson = new JsonQueryResponse(msgQueryTaches.body());

				System.out.println(tachesJson);
				//Pour chaque tâches, l'ajouter à son étape.
				for (int i = 0; i < tachesJson.getData().size(); i++)
				{
					JsonObject tache = tachesJson.getData().getJsonObject(i);

					for (int j = 0; j < this.jr.getResult().getJsonArray("etapes").size(); j++)
					{
						JsonObject etape = this.jr.getResult().getJsonArray("etapes").getJsonObject(j);

						if (tache.getString("id_etape").equals(etape.getString("id_etape")))
						{
							tache.remove("id_etape");
							this.jr.getResult().getJsonArray("etapes").getJsonObject(j).put("taches", tache);
						}
					}
				}

				//Pour la requête de réception des modèles 3D.
				requiredEtapes.clear();
				conditionsSqlSB.delete(0, conditionsSqlSB.length());
				for (int i = 0; i < etapesJson.getData().size(); i++)
				{
					JsonObject etape = etapesJson.getData().getJsonObject(i);
					if (i < etapesJson.size())
						conditionsSqlSB.append("Etape.id_etape=? or ");
					else
						conditionsSqlSB.append("Etape.id_etape=?");

					requiredEtapes.add(etape.getInteger("id_etape"));
				}

				//GET_ETAPES_MODELE_3D
				getEtapesAnd3DModeles(conditionsSqlSB, requiredEtapes);
			})
			.onFailure((event) ->
			{
				this.jr.getMessages().add(event.getMessage());
				this.rc.response().end(this.jr.toString());
			});

		return promise.future();

	}

	private Future<Void> getEtapesAnd3DModeles(StringBuilder conditionsSqlSB, JsonArray requiredEtapes)
	{
		Promise promise = Promise.promise();

		this.vertx.eventBus().request("sql_queries", new JsonObject()
			.put("query", "SELECT Etape_Modele_3D.id_etape, Modele_3D.id_modele_3D, Modele_3D.id_modele_3D, Modele_3D.description_modele_3D, Modele_3D.coordonnees_json "
				+ "FROM Etape_Modele_3D "
				+ "INNER JOIN Etape ON Etape.id_etape = Etape_Modele_3D.id_modele_3D "
				+ "INNER JOIN Modele_3D ON Modele_3D.id_modele_3D = Etape_Modele_3D.id_modele_3D "
				+ "WHERE " + conditionsSqlSB.toString() + ";"
			)
			.put("query_params", requiredEtapes)
		)
			.onSuccess((msgQuery3DModels) ->
			{
				JsonQueryResponse modeles3DJson = new JsonQueryResponse(msgQuery3DModels.body());

				//Pour chaque modele, l'ajouter à son étape.
				for (int i = 0; i < modeles3DJson.getData().size(); i++)
				{
					JsonObject modele = modeles3DJson.getData().getJsonObject(i);

					for (int j = 0; j < this.jr.getResult().getJsonArray("etapes").size(); j++)
					{
						JsonObject etape = this.jr.getResult().getJsonArray("etapes").getJsonObject(j);

						if (modele.getString("id_etape").equals(etape.getString("id_etape")))
						{
							if (!etape.containsKey("3D_models"))
							{
								etape.put("3D_models", new JsonArray());
							}

							this.jr.getResult().getJsonArray("etapes").getJsonObject(j).getJsonArray("3D_models").add(modele);
						}
					}
				}
				//getEtapesAndMedias
				getEtapesAndMedias(conditionsSqlSB, requiredEtapes);
			})
			.onFailure((event) ->
			{
				this.jr.getMessages().add(event.getMessage());
				this.rc.response().end(this.jr.toString());
			});

		return promise.future();
	}

	private Future<Void> getEtapesAndMedias(StringBuilder conditionsSqlSB, JsonArray requiredEtapes)
	{
		Promise promise = Promise.promise();

		this.vertx.eventBus().request("sql_queries", new JsonObject()
			.put("query", "SELECT * "
				+ "FROM Etape_Media "
				+ "INNER JOIN Etape ON Etape.id_etape = Etape_Media.id_etape "
				+ "INNER JOIN Media ON Media.id_media = Etape_Media.id_media "
				+ "WHERE " + conditionsSqlSB.toString() + ";"
			)
			.put("query_params", requiredEtapes)
		)
			.onSuccess((msgQueryMedias) ->
			{
				JsonQueryResponse medias3DJson = new JsonQueryResponse(msgQueryMedias.body());

				//Pour chaque modele, l'ajouter à son étape.
				for (int i = 0; i < medias3DJson.getData().size(); i++)
				{
					JsonObject media = medias3DJson.getData().getJsonObject(i);

					for (int j = 0; j < this.jr.getResult().getJsonArray("etapes").size(); j++)
					{
						JsonObject etape = this.jr.getResult().getJsonArray("etapes").getJsonObject(j);

						if (media.getString("id_etape").equals(etape.getString("id_etape")))
						{
							if (!etape.containsKey("medias"))
							{
								etape.put("medias", new JsonArray());
							}

							media.remove("id_etape");

							this.jr.getResult().getJsonArray("etapes").getJsonObject(j).getJsonArray("medias").add(media);
						}
					}
				}
				this.jr.setStatus("SUCCESS");
				this.rc.response().end(this.jr.toString());
			})
			.onFailure((event) ->
			{
				this.jr.getMessages().add(event.getMessage());
				this.rc.response().end(this.jr.toString());
			});

		return promise.future();
	}
}
