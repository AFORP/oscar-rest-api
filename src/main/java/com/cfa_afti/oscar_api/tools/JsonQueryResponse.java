/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cfa_afti.oscar_api.tools;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 *
 * @author Axel DUMAS <axel.dumas-ext@kct-france.com>
 */
public class JsonQueryResponse
	extends JsonObject
{
  public JsonQueryResponse()
  {
//	//this. = new JsonObject();
//	this.put("data", new JsonArray())
//		.put("count_rows", 0)
//		.put("modified_rows", 0)
//		.put("status", "SUCCESS/FAILED")
//		.put("message", "Message d'erreur.");
  }

  public JsonQueryResponse(Object json)
  {
	((JsonObject) json).forEach((t) ->
	{
	  this.put(t.getKey(), t.getValue());
	});
  }

  public JsonQueryResponse setStatus(String status)
  {
	this.put("status", status);
	return this;
  }

  public JsonQueryResponse setMessage(String message)
  {
	this.put("message", message);
	return this;
  }

  public JsonQueryResponse setData(JsonArray dataQuery)
  {
	this.put("data", dataQuery);
	return this;
  }

  public JsonQueryResponse setRowCount(int rowCount)
  {
	this.put("row_count", rowCount);
	return this;
  }

  public JsonQueryResponse setModifiedRows(int modifiedRows)
  {
	this.put("modified_rows", modifiedRows);
	return this;
  }

  public JsonArray getData()
  {
	return this.getJsonArray("data");
  }
}
