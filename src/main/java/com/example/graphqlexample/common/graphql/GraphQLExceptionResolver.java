package com.example.graphqlexample.common.graphql;

import com.example.graphqlexample.order.application.OrderNotFoundException;
import com.example.graphqlexample.order.domain.InvalidOrderArgumentException;
import com.example.graphqlexample.order.domain.InvalidOrderStatusTransitionException;
import com.example.graphqlexample.product.application.InsufficientStockException;
import com.example.graphqlexample.product.application.ProductNotFoundException;
import com.example.graphqlexample.product.domain.InvalidProductArgumentException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
class GraphQLExceptionResolver implements DataFetcherExceptionResolver {

    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment env) {
        GraphQLError error = switch (exception) {
            case ProductNotFoundException e -> build(e, env, ErrorType.NOT_FOUND, "PRODUCT_NOT_FOUND");
            case InvalidProductArgumentException e -> build(e, env, ErrorType.BAD_REQUEST, "INVALID_PRODUCT_INPUT");
            case OrderNotFoundException e -> build(e, env, ErrorType.NOT_FOUND, "ORDER_NOT_FOUND");
            case InvalidOrderArgumentException e -> build(e, env, ErrorType.BAD_REQUEST, "INVALID_ORDER_INPUT");
            case InvalidOrderStatusTransitionException e ->
                build(e, env, ErrorType.BAD_REQUEST, "INVALID_ORDER_STATUS_TRANSITION");
            case InsufficientStockException e -> build(e, env, ErrorType.BAD_REQUEST, "STOCK_INSUFFICIENT");
            default -> null;
        };
        return Mono.justOrEmpty(error).map(List::of);
    }

    private GraphQLError build(Exception e, DataFetchingEnvironment env, ErrorType type, String code) {
        return GraphqlErrorBuilder.newError(env)
            .errorType(type)
            .message(e.getMessage())
            .extensions(Map.of("code", code))
            .build();
    }
}
