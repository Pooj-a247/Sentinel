const $=s=>document.querySelector(s), $$=s=>[...document.querySelectorAll(s)];let me=null,evs=[],cases=[],users=[];
async function api(url,opt={}){
 const h={...(opt.headers||{})};
 if(opt.body && !(opt.body instanceof FormData)) h["Content-Type"]="application/json";
 let response;
 try {
   response=await fetch(url,{...opt,headers:h,credentials:"same-origin",cache:"no-store",signal:opt.signal||AbortSignal.timeout(60000)});
 } catch(err) {
   if(err.name==="TimeoutError" || err.name==="AbortError") throw Error("The server took too long to respond. Check the VS Code terminal and try again once.");
   throw Error("Cannot reach Sentinel. Check that the application is running.");
 }
 const data=(response.headers.get("content-type")||"").includes("json")?await response.json():await response.text();
 if(!response.ok) throw Error(data?.message||data?.error||`Request failed ${response.status}`);
 return data;
}
const esc=x=>String(x??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));const date=x=>x?new Date(x).toLocaleString():"—";const pill=x=>`<span class="pill ${esc(x)}">${esc(String(x||"—").replaceAll("_"," "))}</span>`;
function toast(s,bad=false){let t=$("#toast");t.textContent=s;t.classList.remove("hidden","bad");if(bad)t.classList.add("bad");setTimeout(()=>t.classList.add("hidden"),4000)}
function loginView(){ $("#app").classList.add("hidden");$("#login").classList.remove("hidden");me=null}
function appView(u){me=u;$("#login").classList.add("hidden");$("#app").classList.remove("hidden");$("#userName").textContent=u.name;$("#userRole").textContent=u.role;$("#usersNav").classList.toggle("hidden",u.role!=="ADMIN");go("overview");loadAll()}
$("#loginForm").onsubmit=async e=>{e.preventDefault();let f=new FormData(e.currentTarget);try{appView(await api("/api/auth/login",{method:"POST",body:JSON.stringify({username:f.get("username"),password:f.get("password")})}))}catch(err){$("#loginError").textContent=err.message}};
$("#logout").onclick=async()=>{try{await api("/api/auth/logout",{method:"POST"})}finally{loginView()}};async function boot(){try{appView(await api("/api/auth/me"))}catch{loginView()}}
function go(p){if(p==="users"&&me.role!=="ADMIN")return;$$(".page").forEach(x=>x.classList.add("hidden"));$("#"+p).classList.remove("hidden");$$("[data-page]").forEach(x=>x.classList.toggle("active",x.dataset.page===p));$("#pageTitle").textContent=({overview:"Overview",evidence:"Evidence vault",cases:"Cases",transfers:"Transfers",custody:"Chain of custody",alerts:"Integrity alerts",users:"User management"})[p];$("#pageLabel").textContent=$("#pageTitle").textContent.toUpperCase();({overview:loadAll,evidence:loadEvidence,cases:loadCases,transfers:loadTransfers,custody:loadCustody,alerts:loadAlerts,users:loadUsers})[p]?.()}
$$("[data-page]").forEach(b=>b.onclick=()=>go(b.dataset.page));$("#refresh").onclick=loadAll;
async function loadAll(){try{let s=await api("/api/dashboard/stats");$("#stats").innerHTML=[["Total evidence",s.totalEvidence],["Verified",s.verifiedEvidence],["Integrity violations",s.integrityViolations],["Pending transfers",s.pendingTransfers],["Open alerts",s.openAlerts]].map(([k,v])=>`<div class="stat"><span>${k}</span><b>${v}</b><small>Live database count</small></div>`).join("");await loadEvidence();await loadCases();if(!$("#transfers").classList.contains("hidden"))await loadTransfers();if(!$("#alerts").classList.contains("hidden"))await loadAlerts()}catch(e){toast(e.message,true)}}
async function loadEvidence(q=""){evs=await api("/api/evidence?q="+encodeURIComponent(q));$("#evidenceRows").innerHTML=evs.map(e=>`<tr><td><b>${esc(e.name)}</b><div class="sub">${esc(e.evidence_id)}</div></td><td>${esc(e.case_id)}</td><td>${esc(e.evidence_type)}</td><td>${esc(e.custodian_name)}</td><td>${pill(e.status)}</td><td><button class="small" data-detail="${esc(e.evidence_id)}">Details</button><button class="small" data-verify="${esc(e.evidence_id)}">Verify</button><button class="small" data-transfer="${esc(e.evidence_id)}">Transfer</button></td></tr>`).join("")||'<tr><td colspan="6" class="empty">No evidence yet.</td></tr>';$("#recent").innerHTML=evs.slice(0,5).map(e=>`<tr><td>${esc(e.name)}<div class="sub">${esc(e.evidence_id)}</div></td><td>${esc(e.case_id)}</td><td>${esc(e.custodian_name)}</td><td>${pill(e.status)}</td><td><button class="small" data-detail="${esc(e.evidence_id)}">Details</button></td></tr>`).join("")||'<tr><td colspan="5" class="empty">No evidence registered.</td></tr>';$("#custodySelect").innerHTML='<option value="">Choose evidence…</option>'+evs.map(e=>`<option value="${esc(e.evidence_id)}">${esc(e.evidence_id)} — ${esc(e.name)}</option>`).join("");updateCaseSelect()}
$("#doSearch").onclick=()=>loadEvidence($("#search").value);$("#search").onkeydown=e=>{if(e.key==="Enter")loadEvidence(e.target.value)};
async function loadCases(){cases=await api("/api/cases");$("#caseCards").innerHTML=cases.map(c=>`<article class="case"><span class="pill">${esc(c.case_id)}</span><h4>${esc(c.title)}</h4><p>${esc(c.description||"No description")}</p><small>${Number(c.evidence_count)||0} evidence items · ${pill(c.status)}</small></article>`).join("")||'<div class="panel empty">Create a case before registering evidence.</div>';updateCaseSelect()}
function updateCaseSelect(){if($("#caseSelect"))$("#caseSelect").innerHTML='<option value="">Choose open case</option>'+cases.filter(c=>c.status!=="CLOSED").map(c=>`<option value="${esc(c.case_id)}">${esc(c.case_id)} — ${esc(c.title)}</option>`).join("")}

$("#showEvidence").onclick = async () => {
    await loadCases();
    $("#evidenceForm").classList.remove("hidden");
};

$("#cancelEvidence").onclick = () =>
    $("#evidenceForm").classList.add("hidden");

$("#evidenceForm").onsubmit = async (e) => {
    e.preventDefault();

    const form = e.currentTarget;

    const submit = form.querySelector('button[type="submit"],button:not([type])');
    if (submit) { submit.disabled = true; submit.textContent = "Registering…"; }
    try {
        const out = await api("/api/evidence", {
            method: "POST",
            body: new FormData(form)
        });

        form.reset();
        form.classList.add("hidden");
        toast("Evidence registered and original hash stored.");

        await loadAll();

        if (out.evidenceId) {
            await detail(out.evidenceId);
        }
    } catch (err) {
        toast(err.message, true);
    } finally {
        if (submit) { submit.disabled = false; submit.textContent = "Register and hash"; }
    }
};

$("#showCase").onclick=()=>$("#caseForm").classList.remove("hidden");$("#cancelCase").onclick=()=>$("#caseForm").classList.add("hidden");$("#caseForm").onsubmit=async e=>{e.preventDefault();let f=new FormData(e.currentTarget);try{await api("/api/cases",{method:"POST",body:JSON.stringify({title:f.get("title"),description:f.get("description")})});e.currentTarget.reset();e.currentTarget.classList.add("hidden");toast("Case created.");await loadCases();await loadEvidence()}catch(err){toast(err.message,true)}};
function modal(s){$("#modalBody").innerHTML=s;$("#modal").classList.remove("hidden")}$("#closeModal").onclick=()=>$("#modal").classList.add("hidden");$("#modal").onclick=e=>{if(e.target.id==="modal")$("#modal").classList.add("hidden")};
async function detail(id){try{let e=await api("/api/evidence/"+encodeURIComponent(id));modal(`<p class="eyebrow">EVIDENCE RECORD</p><h2>${esc(e.name)}</h2><p>${esc(e.evidence_id)} · ${esc(e.case_id)}</p><div class="panel"><p>Status: ${pill(e.status)}</p><p>Custodian: ${esc(e.custodian_name)}</p><p>File: ${esc(e.original_filename)} (${Number(e.file_size).toLocaleString()} bytes)</p><p>Original SHA-256</p><div class="hash">${esc(e.original_hash)}</div><p>${esc(e.description||"No description")}</p><button class="primary" data-verify="${esc(e.evidence_id)}">Verify</button> <button class="secondary" data-transfer="${esc(e.evidence_id)}">Transfer</button></div>`)}catch(e){toast(e.message,true)}}
async function verify(id){try{let r=await api(`/api/evidence/${encodeURIComponent(id)}/verify`,{method:"POST"});modal(`<p class="eyebrow">INTEGRITY CHECK</p><h2 class="${r.match?"good":"bad"}">${r.match?"Integrity verified":"Integrity issue detected"}</h2><p>Status: ${pill(r.status)}</p><p>Original hash</p><div class="hash">${esc(r.originalHash)}</div><p>Current hash</p><div class="hash">${esc(r.currentHash)}</div><p>${r.match?"Hash matches original.":"Alert created; investigate this evidence."}</p>`);await loadAll()}catch(e){toast(e.message,true)}}
async function loadCustody(){if(!evs.length)await loadEvidence();$("#timeline").textContent="Select evidence to view its timeline."}
$("#custodySelect").onchange=async e=>{if(!e.target.value)return;try{let rows=await api(`/api/evidence/${encodeURIComponent(e.target.value)}/custody`);$("#timeline").innerHTML=rows.map(x=>`<div class="timeline-item"><h4>${esc(x.action.replaceAll("_"," "))}</h4><p>${esc(x.details||"")}</p><p>Actor: ${esc(x.actor_name)} ${x.previous_custodian_name?"· From "+esc(x.previous_custodian_name):""} ${x.new_custodian_name?"· To "+esc(x.new_custodian_name):""}</p><small>${date(x.created_at)} · event fingerprint ${esc(String(x.event_hash).slice(0,18))}…</small></div>`).join("")||"No custody events."}catch(err){toast(err.message,true)}};
async function loadTransfers(){let rows=await api("/api/transfers");$("#transferRows").innerHTML=rows.map(t=>`<tr><td>${esc(t.evidence_name)}<div class="sub">${esc(t.evidence_id)}</div></td><td>${esc(t.from_name)}</td><td>${esc(t.to_name)}</td><td>${esc(t.remarks||"—")}</td><td>${pill(t.status)}</td><td>${t.status==="PENDING"&&String(t.to_user)===String(me.id)?`<button class="small" data-respond="${t.transfer_id}" data-accept="true">Accept</button><button class="small" data-respond="${t.transfer_id}" data-accept="false">Reject</button>`:"—"}</td></tr>`).join("")||'<tr><td colspan="6" class="empty">No transfers.</td></tr>'}

async function transfer(id) {
    try {
        const active = await api("/api/recipients");
        if (!active.length) { toast("No active recipients available.", true); return; }
        modal(`<h2>Request transfer</h2><form id="transferForm"><label>Recipient<select name="toUser" required><option value="">Choose recipient</option>${active.map(u => `<option value="${esc(u.id)}">${esc(u.name)} — ${esc(u.role)}</option>`).join("")}</select></label><label>Remarks<textarea name="remarks"></textarea></label><button class="primary">Send transfer</button></form>`);
        $("#transferForm").onsubmit = async e => {
            e.preventDefault();
            const data = new FormData(e.currentTarget);
            const toUser = String(data.get("toUser") || "").trim();
            const remarks = String(data.get("remarks") || "");
            if (!toUser) { toast("Select a recipient first.", true); return; }
            try {
                await api(`/api/evidence/${encodeURIComponent(id)}/transfers`, {method:"POST", body:JSON.stringify({toUser:toUser, recipientId:toUser, remarks:remarks})});
                $("#modal").classList.add("hidden"); toast("Transfer requested.");
                await loadTransfers(); await loadAll();
            } catch (err) { toast(err.message, true); }
        };
    } catch (err) { toast(err.message, true); }
}


async function loadAlerts() {
    try {
    const filter = $("#alertFilter").value;
    const rows = await api("/api/alerts?status=" + filter);

    $("#alertRows").innerHTML = rows.map(a => `
        <tr>
            <td>${esc(a.alert_id)}
                <div class="sub">${esc(a.severity)}</div>
            </td>
            <td>${esc(a.evidence_name || "Unknown")}
                <div class="sub">${esc(a.evidence_id)}</div>
            </td>
            <td>${pill(a.severity)}</td>
            <td>${esc(a.message || "")}</td>
            <td>${pill(a.resolved ? "RESOLVED" : "OPEN")}</td>
            <td>${!a.resolved && me.role === "ADMIN"
                ? `<button class="small" data-resolve="${a.alert_id}">Resolve</button>`
                : "—"}
            </td>
        </tr>
    `).join("") || '<tr><td colspan="6" class="empty">No alerts.</td></tr>';
    } catch (err) { toast(err.message, true); }
}

$("#alertFilter").onchange=loadAlerts;async function loadUsers(){if(me.role!=="ADMIN")return;users=await api("/api/users");$("#userRows").innerHTML=users.map(u=>`<tr><td>${esc(u.name)}</td><td>${esc(u.username)}</td><td>${esc(u.email)}</td><td>${pill(u.role)}</td><td>${esc(u.department||"—")}</td><td>${pill(u.active?"ACTIVE":"INACTIVE")}</td><td><button class="small" data-toggle="${u.id}" data-active="${u.active}">${u.active?"Deactivate":"Activate"}</button></td></tr>`).join("")}
$("#showUser").onclick=()=>$("#userForm").classList.remove("hidden");$("#cancelUser").onclick=()=>$("#userForm").classList.add("hidden");$("#userForm").onsubmit=async e=>{e.preventDefault();try{await api("/api/users",{method:"POST",body:JSON.stringify(Object.fromEntries(new FormData(e.currentTarget)))});e.currentTarget.reset();e.currentTarget.classList.add("hidden");toast("User created.");await loadUsers()}catch(err){toast(err.message,true)}};
document.addEventListener("click",async e=>{let d=e.target.closest("[data-detail]"),v=e.target.closest("[data-verify]"),t=e.target.closest("[data-transfer]"),r=e.target.closest("[data-respond]"),a=e.target.closest("[data-resolve]"),u=e.target.closest("[data-toggle]");if(d)detail(d.dataset.detail);if(v)verify(v.dataset.verify);if(t)transfer(t.dataset.transfer);if(r){try{await api(`/api/transfers/${r.dataset.respond}/respond`,{method:"POST",body:JSON.stringify({accept:r.dataset.accept==="true"})});toast("Transfer response recorded.");await loadTransfers();await loadAll()}catch(err){toast(err.message,true)}}if(a){try{await api(`/api/alerts/${a.dataset.resolve}/resolve`,{method:"POST"});await loadAlerts();await loadAll();toast("Alert resolved")}catch(err){toast(err.message,true)}}if(u){try{await api(`/api/users/${u.dataset.toggle}/active`,{method:"PATCH",body:JSON.stringify({active:u.dataset.active!=="true"})});await loadUsers();toast("User status updated")}catch(err){toast(err.message,true)}}});
boot();
