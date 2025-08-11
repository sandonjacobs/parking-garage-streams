# resource "confluent_connector" "pg_row_aggregate" {
#   environment {
#     id = confluent_environment.cc_env.id
#   }
#
#   kafka_cluster {
#     id = confluent_kafka_cluster.kafka_cluster.id
#   }
#
#   config_sensitive = {
#
#   }
#
#   config_nonsensitive = {
#     "kafka.service.account.id" = confluent_service_account.sa_parking_connectors.id
#
#   }
#
# }


# auto.create                     | true
# auto.evolve                     | true
# cloud.environment               | prod
# cloud.provider                  | aws
# connection.host                 | sjacobs-demo.cluster-c64lt0oz12fw.us-east-2.rds.amazonaws.com
# connection.password             | ****************
# connection.port                 | 5432
# connection.user                 | postgres
# connector.class                 | PostgresSink
# db.name                         | parkinggarage
# input.data.format               | PROTOBUF
# input.key.format                | STRING
# insert.mode                     | UPSERT
# kafka.auth.mode                 | SERVICE_ACCOUNT
# kafka.endpoint                  | SASL_SSL://pkc-921jm.us-east-2.aws.confluent.cloud:9092
# kafka.region                    | us-east-2
# kafka.service.account.id        | sa-j587g5w
# name                            | AggregationsSinkToPostgres
# pk.mode                         | kafka
# table.name.format               | parking.${topic}
# tasks.max                       | 1
# topics                          | parking-row-aggregates,parking-zone-aggregates
# transforms                      | Flattener,RenameFields
# transforms.Flattener.delimiter  | _
# transforms.Flattener.type       | org.apache.kafka.connect.transforms.Flatten$Value
# transforms.RenameFields.renames | carStatus_capacity:car_capacity,carStatus_occupied:car_occupied,handicapStatus_capacity:handicap_capacity,handicapStatus_occupied:handicap_occupied,motorcycleStatus_capacity:motorcycle_capacity,motorcycleStatus_occupied:motorcycle_occupied
# transforms.RenameFields.type    | io.confluent.connect.transforms.ReplaceField$Value