package com.example.graphqlexample.common.graphql;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LocalDateTimeScalar {

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
        .name("LocalDateTime")
        .description("ISO-8601 local date-time (yyyy-MM-dd'T'HH:mm:ss)")
        .coercing(new Coercing<LocalDateTime, String>() {

            @Override
            public String serialize(Object dataFetcherResult) {
                if (dataFetcherResult instanceof LocalDateTime localDateTime) {
                    return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                throw new CoercingSerializeException("Expected a LocalDateTime object");
            }

            @Override
            public LocalDateTime parseValue(Object input) {
                try {
                    return LocalDateTime.parse(input.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    throw new CoercingParseValueException("Invalid LocalDateTime: " + input, e);
                }
            }

            @Override
            public LocalDateTime parseLiteral(Object input) {
                if (input instanceof StringValue stringValue) {
                    return parseValue(stringValue.getValue());
                }
                throw new CoercingParseLiteralException("Expected a StringValue");
            }
        })
        .build();

    private LocalDateTimeScalar() {}
}
