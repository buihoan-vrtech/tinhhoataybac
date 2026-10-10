package vn.edu.crs.tinhhoataybac;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import vn.edu.crs.tinhhoataybac.controller.SavedProductsController;
import vn.edu.crs.tinhhoataybac.model.Product;
import vn.edu.crs.tinhhoataybac.repository.ProductRepository;

class SavedProductsTests {
  @Test void boundsCollectionRequestsAndRejectsInvalidIds() {
    assertEquals(List.of(3L,2L), SavedProductsController.parseIds("3,2,3,-1,0,nope,9223372036854775808"));
    assertTrue(SavedProductsController.parseIds("1".repeat(2201)).isEmpty());
    String ids = IntStream.rangeClosed(1,110).mapToObj(Integer::toString).collect(Collectors.joining(","));
    assertEquals(100,SavedProductsController.parseIds(ids).size());
  }

  @Test void preservesSavedOrderAndHandlesDeletedOrUnpricedProducts() {
    Product a = new Product(); a.setId(1L); a.setName("A"); a.setPrice(100.0); a.setStock(0.0);
    Product b = new Product(); b.setId(2L); b.setName("B"); b.setPrice(200.0); b.setStock(5.0);
    Product invalid = new Product(); invalid.setId(3L); invalid.setName("Invalid");
    ProductRepository repo = (ProductRepository) Proxy.newProxyInstance(ProductRepository.class.getClassLoader(),
        new Class<?>[]{ProductRepository.class}, (proxy,method,args) -> {
          if (method.getName().equals("findAllById")) return List.of(b,invalid,a);
          throw new UnsupportedOperationException(method.getName());
        });
    var result = new SavedProductsController(repo).collection("1,99,2,3");
    assertEquals(List.of(1L,2L),result.stream().map(SavedProductsController.Card::id).toList());
    assertFalse(result.get(0).available()); assertTrue(result.get(1).available());
    assertEquals("Đặc sản Tây Bắc",result.get(0).category());
  }
}
