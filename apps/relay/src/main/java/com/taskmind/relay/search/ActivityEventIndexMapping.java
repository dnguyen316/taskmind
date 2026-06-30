package com.taskmind.relay.search;

/** OpenSearch-compatible mapping for the activity-events recommendation index. */
public final class ActivityEventIndexMapping {
    public static final String MAPPING_JSON =
            """
            {
              "mappings": {
                "properties": {
                  "eventId": {"type": "keyword"},
                  "eventType": {"type": "keyword", "normalizer": "lowercase_keyword"},
                  "eventTypeKeyword": {"type": "keyword", "normalizer": "lowercase_keyword"},
                  "actorUserId": {"type": "keyword"},
                  "userId": {"type": "keyword"},
                  "projectId": {"type": "keyword"},
                  "entityType": {"type": "keyword", "normalizer": "lowercase_keyword"},
                  "entityTypeKeyword": {"type": "keyword", "normalizer": "lowercase_keyword"},
                  "entityId": {"type": "keyword"},
                  "title": {
                    "type": "text",
                    "fields": {
                      "keyword": {"type": "keyword", "normalizer": "lowercase_keyword"},
                      "autocomplete": {"type": "search_as_you_type"}
                    }
                  },
                  "status": {"type": "keyword", "normalizer": "lowercase_keyword"},
                  "statusKeyword": {"type": "keyword", "normalizer": "lowercase_keyword"},
                  "taskTypeKey": {"type": "keyword", "normalizer": "lowercase_keyword"},
                  "taskTypeName": {"type": "keyword", "normalizer": "lowercase_keyword"},
                  "taskTypeColor": {"type": "keyword"},
                  "payloadText": {"type": "text"},
                  "payload": {"type": "object", "enabled": false},
                  "occurredAt": {"type": "date"}
                }
              },
              "settings": {
                "analysis": {
                  "normalizer": {
                    "lowercase_keyword": {
                      "type": "custom",
                      "filter": ["lowercase", "asciifolding"]
                    }
                  }
                }
              }
            }
            """;

    private ActivityEventIndexMapping() {}
}
