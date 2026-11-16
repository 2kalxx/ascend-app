// node lingua/validate_course.js lingua/courses/fr.js  → checks shape of a course pack
const fs=require('fs'); global.COURSES={};
for(const f of process.argv.slice(2)){
  eval(fs.readFileSync(f,'utf8'));
}
let bad=0;
for(const [code,c] of Object.entries(COURSES)){
  const errs=[];
  if(!c.lang||!c.tts) errs.push('missing lang/tts');
  if(!Array.isArray(c.units)||c.units.length!==30) errs.push('need 30 units, got '+(c.units||[]).length);
  (c.units||[]).forEach((u,i)=>{
    if(!u.w||u.w.length!==12) errs.push(`unit ${i+1}: need 12 words, got ${(u.w||[]).length}`);
    if(!u.s||u.s.length!==8) errs.push(`unit ${i+1}: need 8 sentences, got ${(u.s||[]).length}`);
    (u.w||[]).concat(u.s||[]).forEach(x=>{ if(!Array.isArray(x)||x.length<2||!x[0]||!x[1]) errs.push(`unit ${i+1}: bad entry ${JSON.stringify(x)}`); });
  });
  const all=(c.units||[]).flatMap(u=>(u.w||[]).map(w=>w[0])); const d=[...new Set(all.filter((w,i)=>all.indexOf(w)!==i))];
  if(d.length) errs.push('duplicate words: '+d.join(', '));
  console.log(code, errs.length?'FAIL':'OK', errs.slice(0,10).join(' | ')); if(errs.length) bad++;
}
process.exit(bad?1:0);
