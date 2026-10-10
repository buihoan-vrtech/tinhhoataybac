document.addEventListener('DOMContentLoaded',()=>{
 const form=document.querySelector('form[action="/checkout/place-order"]');if(!form)return;
 let timer,controller,version=0;
 const submit=form.querySelector('button[type="submit"]');
 const error=document.getElementById('quote-error');
 const money=n=>new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(n);
 async function quote(){
  const current=++version;controller?.abort();controller=new AbortController();
  if(submit)submit.disabled=true;
  error.textContent='Đang cập nhật tổng tiền…';
  try{
   const response=await fetch('/checkout/quote',{method:'POST',body:new FormData(form),signal:controller.signal});
   const q=await response.json();if(current!==version)return;
   if(!response.ok)throw new Error(q.error||'Chưa tính được tổng tiền.');
   for(const [id,value] of [['subtotal',q.subtotal],['shipping',q.shippingFee],['gift',q.giftWrapFee||0],['discount',q.discount],['total',q.total]])document.getElementById('quote-'+id).textContent=money(value);
   for(const [id,value] of [['shipping',q.shippingFee],['gift',q.giftWrapFee||0],['discount',q.discount],['total',q.total]]){const el=document.getElementById('summary-'+id);if(el)el.textContent=money(value);} error.textContent='';if(submit)submit.disabled=false;
  }catch(e){if(current!==version||e.name==='AbortError')return;document.getElementById('quote-total').textContent='Chưa tính được';error.textContent=e.message||'Chưa tải được tổng tiền. Vui lòng thử lại.';}
 }
 for(const name of ['shippingMethod','giftWrapId'])form.querySelector('[name='+name+']')?.addEventListener('change',quote);
 form.querySelector('[name=voucherCode]')?.addEventListener('input',()=>{clearTimeout(timer);if(submit)submit.disabled=true;timer=setTimeout(quote,350);});
 quote();
});
