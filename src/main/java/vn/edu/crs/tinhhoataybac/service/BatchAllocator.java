package vn.edu.crs.tinhhoataybac.service;
import java.util.*;
import vn.edu.crs.tinhhoataybac.model.*;
public final class BatchAllocator{
 public record Allocation(ProductBatch batch,double quantity){}
 public static List<Allocation> allocate(Product p,double quantity){
  p.validateQuantity(quantity);
  if(!p.getBatchTracked())return List.of(new Allocation(null,quantity));
  var available=p.getSaleBatches();
  if(available.stream().mapToDouble(ProductBatch::getRemainingQuantity).sum()+0.000001<quantity)throw new IllegalStateException("Không đủ hàng còn hạn sử dụng.");
  List<Allocation> result=new ArrayList<>();double left=quantity;
  for(var b:available){if(left<=0)break;double take=Math.min(left,b.getRemainingQuantity());b.setRemainingQuantity(Math.round((b.getRemainingQuantity()-take)*100)/100.0);result.add(new Allocation(b,take));left=Math.round((left-take)*100)/100.0;}
  return result;
 }
}
