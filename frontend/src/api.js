export async function api(path,options={}){
  const response=await fetch(path,{headers:{'Content-Type':'application/json',...(options.headers||{})},...options});
  let body=null;try{body=await response.json()}catch{}
  if(!response.ok) throw {status:response.status,...(body||{message:'Request failed'})};
  return body;
}
