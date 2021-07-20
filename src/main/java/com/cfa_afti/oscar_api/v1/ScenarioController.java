/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.v1;

import com.cfa_afti.oscar_api.tools.JsonQueryResponse;
import com.cfa_afti.oscar_api.tools.JsonResponse;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ScenarioController
	implements Handler<RoutingContext>
{
  private final Vertx vertx;
  private final JsonResponse jr;
  private RoutingContext rc;

  public ScenarioController(Vertx vertx)
  {
	this.vertx = vertx;
	this.jr = new JsonResponse();
  }

  @Override
  public void handle(RoutingContext rc)
  {
	this.rc = rc;

	rc.response().putHeader("Content-Type", "application/json");

	if (rc.request().method() == HttpMethod.GET)
	{
	  if (rc.request().params().size() == 0)
	  {
		getScenarios();
	  }
	  else if (rc.request().getParam("groupe") != null)
	  {
		getScenariosFromGroup(rc.request().getParam("groupe"));
	  }
	  else if (rc.request().getParam("id") != null)
	  {
		try
		{
		  getScenario(rc.request().getParam("id"));
		}
		catch (Exception e)
		{
		  e.printStackTrace();
		}

	  }
	}

//	this.rc = rc;
//
//	if (rc.request().method() == HttpMethod.GET)
//	{
//	  if (rc.request().params().size() == 0)
//	  {
//		getScenario();
//	  }
//	  else if (rc.request().getParam("groupe") != null)
//	  {
//		getScenariosFromGroup(rc.request().getParam("groupe"));
//	  }
//	  else if (rc.request().getParam("id") != null && rc.request().getParam("full").equals("true"))
//	  {
//		getScenarioComplete(rc.request().getParam("id"));
//	  }
//	  else if (rc.request().getParam("id") != null)
//	  {
//		getScenario(rc.request().getParam("id"));
//	  }
//	  else
//	  {
//		System.out.println("GET /scenario/ inconnu.");
//	  }
//	}
//	else if (rc.request().method() == HttpMethod.POST)
//	  if (rc.request().getParam("data") != null)
//	  {
//		postScenario(rc.request().getParam("data"));
//	  }
//  }
//  else if (rc.request ()
//
//
//  .method() == HttpMethod.DELETE)
//  {
//
//  }
//
//
//	else
//  {
//	System.out.println("Unsupported HTTP method.");
//  }
  }

  private void postScenario(String data)
  {
	try
	{
	  JsonObject scenarioData = new JsonObject(data);
	  this.vertx.eventBus().request(
		  "sql_queries",
		  new JsonObject()
			  .put("query", "INSERT INTO Scenario VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
			  .put("query_params", new JsonArray()
				  .add(scenarioData.getInteger("id_scenario"))
				  .add(scenarioData.getString("titre_scenario"))
				  .add(scenarioData.getString("description_scenario"))
				  .add(scenarioData.getString("fin_scenario"))
				  .add(scenarioData.getInteger("publie_scenario"))
				  .add(scenarioData.getInteger("id_groupe"))
				  .add(scenarioData.getInteger("id_user"))
				  .add(scenarioData.getInteger("id_localisation"))))
		  .onSuccess((msg) ->
		  {
			this.jr.setStatus("SUCCESS");
			this.jr.getMessages().add("Insertion du scénario réussie.");
			this.jr.setResult(new JsonObject().put("data", (JsonArray) msg.body()));
		  })
		  .onFailure((event) ->
		  {
			this.jr.setStatus("FAILED");
			this.jr.getMessages().add("Insertion du scénario echouée.");
		  });
	}
	catch (Exception e)
	{
	  e.printStackTrace();
	}
  }
  private void deleteScenario()
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
		})
		.onFailure((event) ->
		{
		  this.jr.setStatus("FAILED");
		  this.jr.getMessages().add("suppression du scénario echouée.");
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
		  this.jr.getResult().put("sql_result", (JsonArray) msg.body());

		  if (((JsonArray) msg.body()).size() == 0)
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

  private void getScenariosFromGroup(String sIdGroupe)
  {
	this.vertx.eventBus().request("sql_queries", new JsonObject()
		.put("query", "SELECT Scenario.id_scenario, Scenario.titre_scenario, Groupe.id_groupe, Groupe.label_groupe FROM Scenario INNER JOIN Groupe ON Scenario.id_groupe = Groupe.id_groupe WHERE Groupe.id_groupe=?;")
		.put("query_params", new JsonArray().add(sIdGroupe)))
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

  private void getScenario(String id)
  {
	this.vertx.eventBus().request("sql_queries", new JsonObject()
		.put("query", """
      SELECT * FROM Scenario
      INNER JOIN Localisation ON Localisation.id_localisation = Scenario.id_localisation WHERE id_scenario=?;""")
		.put("query_params", new JsonArray().add(id)))
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

		  this.vertx.eventBus().request("sql_queries", new JsonObject()
			  .put("query", "SELECT * FROM Etape WHERE id_scenario=? ORDER BY num_etape ASC;")
			  .put("query_params", new JsonArray().add(id)))
			  .onSuccess((msgQueryEtapes) ->
			  {
				JsonQueryResponse etapesJson = new JsonQueryResponse(msgQueryEtapes.body());
				StringBuilder conditionsSqlSB = new StringBuilder();
				JsonArray requiredEtapes = new JsonArray();

				//Pour la requete de réception de tâches.
				for (int i = 0; i < etapesJson.getData().size(); i++)
				{
				  JsonObject etape = etapesJson.getData().getJsonObject(i);
				  if (i < etapesJson.size() - 2)
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
				}
				else
				{
				  this.vertx.eventBus().request("sql_queries", new JsonObject()
					  .put("query", "SELECT * FROM Tache WHERE " + conditionsSqlSB.toString()
						  + " ORDER BY ordre_tache ASC;")
					  .put("query_params", requiredEtapes))
					  .onSuccess((msgQueryTaches) ->
					  {
						JsonQueryResponse tachesJson = new JsonQueryResponse(msgQueryTaches.body());

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

						//Pour la requête de réception des modèles 3D tâches.
						requiredEtapes.clear();
						conditionsSqlSB.delete(0, conditionsSqlSB.length());
						for (int i = 0; i < etapesJson.getData().size(); i++)
						{
						  JsonObject etape = etapesJson.getData().getJsonObject(i);
						  if (i < etapesJson.size() - 2)
							conditionsSqlSB.append("Etape.id_etape=? or ");
						  else
							conditionsSqlSB.append("Etape.id_etape=?");

						  requiredEtapes.add(etape.getInteger("id_etape"));
						}

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
							})
							.onFailure((event) ->
							{
							  this.jr.getMessages().add(event.getMessage());
							  this.rc.response().end(this.jr.toString());
							});
					  })
					  .onFailure((event) ->
					  {
						this.jr.getMessages().add(event.getMessage());
						this.rc.response().end(this.jr.toString());
					  });
				}

			  })
			  .onFailure((event) ->
			  {
				this.jr.getMessages().add(event.getMessage());
				this.rc.response().end(this.jr.toString());
			  });
		})
		.onFailure((event) ->
		{
		  this.jr.setHttpCode(this.rc.response().getStatusCode());
		  this.jr.setStatus("FAILED");
		  this.jr.getMessages().add(event.getMessage());
		  this.rc.response().end(this.jr.toString());
		});
  }
}
