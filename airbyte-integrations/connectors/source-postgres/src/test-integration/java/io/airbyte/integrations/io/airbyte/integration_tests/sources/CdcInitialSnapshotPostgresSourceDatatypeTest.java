/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.io.airbyte.integration_tests.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.airbyte.commons.json.Jsons;
import io.airbyte.db.Database;
import io.airbyte.db.factory.DSLContextFactory;
import io.airbyte.db.factory.DatabaseDriver;
import io.airbyte.db.jdbc.JdbcUtils;
import io.airbyte.integrations.standardtest.source.TestDataHolder;
import io.airbyte.integrations.standardtest.source.TestDestinationEnv;
import io.airbyte.integrations.util.HostPortResolver;
import io.airbyte.protocol.models.JsonSchemaPrimitiveUtil;
import io.airbyte.protocol.models.JsonSchemaType;
import java.util.List;
import org.jooq.SQLDialect;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import com.fasterxml.jackson.databind.JsonNode;
import io.airbyte.cdk.db.Database;
import io.airbyte.cdk.integrations.standardtest.source.TestDataHolder;
import io.airbyte.integrations.source.postgres.PostgresTestDatabase;
import io.airbyte.integrations.source.postgres.PostgresTestDatabase.BaseImage;
import io.airbyte.integrations.source.postgres.PostgresTestDatabase.ContainerModifier;
import io.airbyte.protocol.models.JsonSchemaType;

public class CdcInitialSnapshotPostgresSourceDatatypeTest extends AbstractPostgresSourceDatatypeTest {

  private static final String SCHEMA_NAME = "test";

  @Override
  protected Database setupDatabase() throws Exception {
    testdb = PostgresTestDatabase.in(BaseImage.POSTGRES_16, ContainerModifier.CONF)
        .with("CREATE EXTENSION hstore;")
        .with("CREATE SCHEMA TEST;")
        .with("CREATE TYPE mood AS ENUM ('sad', 'ok', 'happy');")
        .with("CREATE TYPE inventory_item AS (\n"
            + "    name            text,\n"
            + "    supplier_id     integer,\n"
            + "    price           numeric\n"
            + ");")
        .with("SET TIMEZONE TO 'MST'")
        .withReplicationSlot()
        .withPublicationForAllTables();
    return testdb.getDatabase();
  }

  @Override
  protected JsonNode getConfig() {
    return testdb.integrationTestConfigBuilder()
        .withSchemas(SCHEMA_NAME)
        .withoutSsl()
        .withCdcReplication()
        .build();
  }

  @Override
  protected void tearDown(final TestDestinationEnv testEnv) {
    dslContext.close();
    container.close();
  }

  @Override
  protected void addJsonbArrayTest() {

    addDataTypeTestData(
        TestDataHolder.builder()
            .sourceType("jsonb_array")
            .fullSourceDataType("JSONB[]")
            .airbyteType(JsonSchemaType.builder(JsonSchemaPrimitiveUtil.JsonSchemaPrimitive.ARRAY)
                .withItems(JsonSchemaType.JSONB)
                .build())
            .addInsertValues(
                "ARRAY['[1,2,1]', 'false']::jsonb[]",
                "ARRAY['{\"letter\":\"A\", \"digit\":30}', '{\"letter\":\"B\", \"digit\":31}']::jsonb[]")
            .addExpectedValues("[\"[1, 2, 1]\",\"false\"]",
                "[\"{\\\"digit\\\": 30, \\\"letter\\\": \\\"A\\\"}\",\"{\\\"digit\\\": 31, \\\"letter\\\": \\\"B\\\"}\"]")
            .build());
  }

  public boolean testCatalog() {
    return true;
  }
    @Override
    protected void addHstoreTest() {
        addDataTypeTestData(
                TestDataHolder.builder()
                        .sourceType("hstore")
                        .airbyteType(JsonSchemaType.STRING)
                        .addInsertValues("""
                             '"paperback" => "243","publisher" => "postgresqltutorial.com",
                             "language"  => "English","ISBN-13" => "978-1449370000",
                             "weight"    => "11.2 ounces"'
                             """, null)
                        .addExpectedValues(
                                //
                                "\"weight\"=>\"11.2 ounces\", \"ISBN-13\"=>\"978-1449370000\", \"language\"=>\"English\", \"paperback\"=>\"243\", \"publisher\"=>\"postgresqltutorial.com\"",
                                null)
                        .build());
    }

}
