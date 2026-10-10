package vn.edu.crs.tinhhoataybac;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.service.CatalogBrowser;

class CatalogBrowserTests {
  private Product product(long id, String name, double price, double stock, long categoryId) {
    Product p = new Product();
    p.setId(id); p.setName(name); p.setPrice(price); p.setStock(stock);
    Category c = new Category(); c.setId(categoryId); p.setCategory(c);
    return p;
  }

  @Test void matchesVietnameseWithoutAccentsAndCombinesFilters() {
    var a = product(1,"Miến dong Tây Bắc",65000,10,5);
    var b = product(2,"Miến dong khô",70000,0,5);
    var c = product(3,"Miến dong",60000,10,6);
    var result = CatalogBrowser.browse(List.of(a,b,c),"MIEN DONG",5L,60000.0,66000.0,true,false,"newest",0);
    assertEquals(List.of(a),result.items());
    assertEquals("dac san",CatalogBrowser.normalize("Đặc sản"));
  }

  @Test void usesDiscountedPriceForFilteringAndOrdering() {
    var a = product(1,"A",200000,10,5); a.setPromotionPrice(50000.0);
    var b = product(2,"B",80000,10,5);
    assertEquals(List.of(a,b),CatalogBrowser.browse(List.of(b,a),"",null,null,100000.0,false,false,"price_asc",0).items());
    assertEquals(List.of(a),CatalogBrowser.browse(List.of(b,a),"",null,null,null,false,true,"newest",0).items());
  }

  @Test void clampsPagesAndKeepsFilteredTotals() {
    List<Product> list = new ArrayList<>();
    for(long i=1;i<=14;i++) list.add(product(i,"Gạo "+i,50000,10,6));
    var last = CatalogBrowser.browse(list,"gao",6L,null,null,false,false,"newest",Integer.MAX_VALUE);
    assertEquals(14,last.total()); assertEquals(2,last.pages()); assertEquals(1,last.page());
    assertEquals(5,last.items().size()); assertEquals(10,last.first()); assertEquals(14,last.last());
    assertEquals(5L,last.items().getFirst().getId());
  }

  @Test void emptyResultsAndInvalidBoundsAreHandled() {
    var a=product(1,"A",100,1,5);
    var empty=CatalogBrowser.browse(List.of(a),"missing",null,null,null,false,false,"invalid",-10);
    assertEquals(0,empty.total()); assertEquals(0,empty.first()); assertEquals(1,empty.pages());
    assertNull(CatalogBrowser.priceBound(Double.NaN));
    assertTrue(CatalogBrowser.browse(List.of(a),"",null,200.0,50.0,false,false,"newest",0).items().isEmpty());
  }
}
