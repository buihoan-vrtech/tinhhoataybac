package vn.edu.crs.tinhhoataybac;
import org.junit.jupiter.api.Test;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.service.BatchAllocator;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class BatchAllocatorTests{
 ProductBatch batch(long id,int days,double q){var b=new ProductBatch();b.setId(id);b.setBatchCode("LOT"+id);b.setManufacturedOn(LocalDate.now().minusDays(5));b.setExpiresOn(LocalDate.now().plusDays(days));b.setRemainingQuantity(q);return b;}
 @Test void reservesEarliestExpiryAndNeverExpiredInventory(){var p=new Product();p.setBatchTracked(true);var expired=batch(1,-1,10);var later=batch(2,30,5);var early=batch(3,5,2);p.getBatches().addAll(java.util.List.of(expired,later,early));assertEquals(7,p.getStock());var a=BatchAllocator.allocate(p,3);assertEquals(3,a.get(0).batch().getId());assertEquals(2,a.get(0).quantity());assertEquals(1,a.get(1).quantity());assertEquals(10,expired.getRemainingQuantity());assertEquals(4,p.getStock());}
 @Test void failureDoesNotConsumePartialStock(){var p=new Product();p.setBatchTracked(true);var b=batch(1,7,2);p.getBatches().add(b);assertThrows(IllegalStateException.class,()->BatchAllocator.allocate(p,3));assertEquals(2,b.getRemainingQuantity());}
 @Test void snapshotsRemainAfterBatchMetadataChanges(){var b=batch(1,7,2);var detail=new OrderDetail();detail.setBatch(b);var expiry=b.getExpiresOn();b.setBatchCode("CHANGED");b.setExpiresOn(expiry.plusDays(7));assertEquals("LOT1",detail.getBatchCodeSnapshot());assertEquals(expiry,detail.getExpirySnapshot());}
 @Test void oldInventoryStillWorksAndFutureProductionCannotSell(){var p=new Product();p.setStock(12d);assertEquals(12,p.getStock());assertNull(BatchAllocator.allocate(p,1).get(0).batch());var b=batch(1,30,2);b.setManufacturedOn(LocalDate.now().plusDays(1));assertFalse(b.isSellable(LocalDate.now()));}
}
