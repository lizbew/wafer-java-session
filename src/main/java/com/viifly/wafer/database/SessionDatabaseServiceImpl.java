package com.viifly.wafer.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


/**
 * Created on 2018/1/3.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
public class SessionDatabaseServiceImpl implements SessionDatabaseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionDatabaseServiceImpl.class);

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final static String SQL_CREATE_SESSION_TABLE = "CREATE TABLE IF NOT EXISTS cSessionInfo ("
            + "id integer identity primary key,"
            + "uuid varchar(100)  NOT NULL, "
            + "skey varchar(100)  NOT NULL, "
            + "create_time datetime NOT NULL, "
            + "last_visit_time datetime NOT NULL, "
            + "open_id varchar(100)  NOT NULL, "
            + "session_key varchar(100)  NOT NULL, "
            + "user_info varchar(2048) NOT NULL"
            //+ "PRIMARY KEY (id),"
            //+ "KEY auth (uuid,skey), "
            //+ "KEY weixin (open_id,session_key) "
            + ")";
    private final static String SQL_HAS_USER = "select count(open_id) as hasUser from cSessionInfo  where open_id = ?";

    private final static String SQL_UPDATE_SESSION = "update cSessionInfo set uuid=?, "
            + "skey=? , create_time=? , last_visit_time=?,"
            + "session_key=?, user_info=?"
            + " where open_id = ?";

    private final static String SQL_INSERT_SESSION = "insert into cSessionInfo (uuid,skey, create_time, last_visit_time, open_id, session_key, user_info) "
            + "values(?, ?, ?, ?, ?, ?, ?)";
    private final static String SQL_USER_INFO_BY_SKEY = "select * from  cSessionInfo where skey=?";

    private final JDBCClient dbClient;

    public SessionDatabaseServiceImpl(JDBCClient dbClient, Handler<AsyncResult<SessionDatabaseService>> readyHandler) {
        this.dbClient = dbClient;

        dbClient.getConnection(ar -> {
            if (ar.failed()) {
                LOGGER.error("Could not open a database connection", ar.cause());
                readyHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();
                connection.execute(SQL_CREATE_SESSION_TABLE, create -> {
                    connection.close();
                    if (create.failed()) {
                        LOGGER.error("Database preparation error", create.cause());
                        readyHandler.handle(Future.failedFuture(create.cause()));
                    } else {
                        readyHandler.handle(Future.succeededFuture(this));
                    }
                });
            }
        });

    }

    @Override
    public SessionDatabaseService saveUserInfo(JsonObject decryptedData, String skey, String sessionKey, Handler<AsyncResult<JsonObject>> resultHandler) {
        String uuid = UUID.randomUUID().toString();
        String now = sdf.format(new Date());

        JsonObject resultJson = new JsonObject();
        resultJson.put("id", uuid);
        resultJson.put("userInfo", decryptedData);
        resultJson.put("skey", skey);

        String openId = decryptedData.getString("openId");
        this.hasUserByOpenId(openId, hasUser -> {
            if (hasUser.succeeded()) {
                if (hasUser.result() == 0) {
                    JsonArray params = new JsonArray()
                            .add(uuid).add(skey).add(now).add(now)
                            .add(openId)
                            .add(sessionKey)
                            .add(decryptedData.toString());
                    runUpdateSql(SQL_INSERT_SESSION, params, update -> {
                        if (update.succeeded()) {
                            resultHandler.handle(Future.succeededFuture(resultJson));
                        } else {
                            LOGGER.error("Database UPDATE error", update.cause());
                            resultHandler.handle(Future.failedFuture(update.cause()));
                        }
                    });
                } else {
                    JsonArray params2 = new JsonArray()
                            .add(uuid).add(skey).add(now).add(now)
                            .add(sessionKey)
                            .add(decryptedData.toString())
                            .add(openId);
                    runUpdateSql(SQL_UPDATE_SESSION, params2, update -> {
                        if (update.succeeded()) {
                            resultHandler.handle(Future.succeededFuture(resultJson));
                        } else {
                            LOGGER.error("Database UPDATE error", update.cause());
                            resultHandler.handle(Future.failedFuture(update.cause()));
                        }
                    });
                }
            } else {
                LOGGER.error("Database query error", hasUser.cause());
                resultHandler.handle(Future.failedFuture(hasUser.cause()));
            }
        });
        return this;
    }

    private SessionDatabaseService hasUserByOpenId(String openId, Handler<AsyncResult<Integer>> resultHandler) {
        dbClient.queryWithParams(SQL_HAS_USER, new JsonArray().add(openId), fetch -> {
            if (fetch.succeeded()) {
                int count = 0;
                ResultSet resultSet = fetch.result();
                if (resultSet.getNumRows() > 0) {
                    JsonArray row = resultSet.getResults().get(0);
                    count = row.getInteger(0);
                }
                resultHandler.handle(Future.succeededFuture(new Integer(count)));
            } else {
                LOGGER.error("Database query error", fetch.cause());
                resultHandler.handle(Future.failedFuture(fetch.cause()));
            }
        });
        return this;
    }

    private SessionDatabaseService runUpdateSql(String sql, JsonArray params, Handler<AsyncResult<Integer>> resultHandler) {
        dbClient.updateWithParams(sql, params, updateResult -> {
            if (updateResult.succeeded()) {
                UpdateResult result = updateResult.result();
                resultHandler.handle(Future.succeededFuture(result.getUpdated()));
            } else {
                LOGGER.error("Database UPDATE error", updateResult.cause());
                resultHandler.handle(Future.failedFuture(updateResult.cause()));
            }
        });
        return this;
    }

    @Override
    public SessionDatabaseService getUserInfoBySKey(String skey, Handler<AsyncResult<JsonObject>> resultHandler) {
        JsonArray params = new JsonArray().add(skey);
        dbClient.queryWithParams(SQL_USER_INFO_BY_SKEY, params, query -> {
            if (query.succeeded()) {
                ResultSet resultSet = query.result();
                if (resultSet.getNumRows() > 0) {
                    JsonObject result = resultSet.getRows().get(0);
                    result.put("found", true);
                    /*
                    JsonObject ret = new JsonObject()
                            .put("found", true)
                            .put("")
                            */
                    resultHandler.handle(Future.succeededFuture(result));
                } else {
                    resultHandler.handle(Future.succeededFuture(
                            new JsonObject().put("found", false)));
                }

            } else {
                LOGGER.error("Database query error", query.cause());
                resultHandler.handle(Future.failedFuture(query.cause()));
            }
        });
        return this;
    }

}

