package com.example.graphqlexample.product.application;

import com.example.graphqlexample.product.domain.Product;
import com.example.graphqlexample.product.domain.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCommandService {

    private final ProductRepository productRepository;

    public void createProduct(String name, BigDecimal price, int stock) {
        Product product = Product.create(name, price, stock);
        productRepository.save(product);
    }

    // REQUIRES_NEW: called from an AFTER_COMMIT listener where the original transaction
    // has already closed, so joining it (the class-level REQUIRED default) fails with
    // "No active transaction" — this must always open its own fresh transaction.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(List<StockChange> items) {
        for (StockChange item : items) {
            long updated = productRepository.decreaseStockIfSufficient(item.productId(), item.quantity());
            if (updated == 0) {
                throw new InsufficientStockException(item.productId());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreStock(List<StockChange> items) {
        for (StockChange item : items) {
            productRepository.increaseStock(item.productId(), item.quantity());
        }
    }

    public void updateProduct(Long id, Product.UpdateCommand command) {
        Product product = getProductInternal(id);
        product.update(command);
    }

    public void deleteProduct(Long id) {
        Product product = getProductInternal(id);
        product.delete();
    }

    private Product getProductInternal(Long id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public record StockChange(Long productId, int quantity) {}
}
