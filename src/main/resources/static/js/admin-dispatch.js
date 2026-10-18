(() => {
    "use strict";
    const element = id => document.getElementById(id);
    if (!element("dispatch")) return;
    const fixedBranch = Number(document.body.dataset.adminBranchId || 0);
    const csrf = document.querySelector('meta[name="_csrf"]')?.content || "";
    const money = value => new Intl.NumberFormat("en-ZA",{style:"currency",currency:"ZAR"}).format(Number(value || 0));
    const esc = value => String(value ?? "").replace(/[&<>"']/g, character => ({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"})[character]);
    const branch = () => fixedBranch || Number(element("dispatchBranch")?.value || 1);
    let snapshot = null;
    let liveClient = null;
    let delay = 1000;
    let cashUpShift = null;
    let cashPreview = null;
    let previewRevision = 0;

    function error(message) {const box=element("dispatchError");box.textContent=message;box.classList.toggle("d-none",!message);}
    async function request(path,method="GET",body) {
        const response=await fetch(path,{method,headers:method==="GET"?{}:{"X-CSRF-TOKEN":csrf,"Content-Type":"application/json"},body:body?JSON.stringify(body):undefined});
        const data=await response.json().catch(()=>({}));
        if(!response.ok)throw new Error(data.error||`Request failed (${response.status})`);
        return data;
    }
    function driverOptions() {return (snapshot?.drivers||[]).filter(d=>d.active&&snapshot.shifts.some(s=>s.driverId===d.id&&s.state==="ACTIVE")).map(d=>`<option value="${d.id}">${esc(d.firstName)} ${esc(d.lastName)}</option>`).join("");}
    function render(data) {
        const selected=new Set(Array.from(document.querySelectorAll('[data-batch-order]:checked')).map(input=>Number(input.value)));
        const selectedDriver=element("dispatchBatchDriver").value;
        snapshot=data;
        const options=driverOptions();
        element("dispatchQueue").innerHTML=data.queue.length?data.queue.map(o=>`<div class="dispatch-row"><div class="d-flex align-items-center"><input type="checkbox" class="form-check-input" data-batch-order value="${o.id}" aria-label="Select order ${o.id}" ${selected.has(o.id)?"checked":""}><div><strong>#${o.id} · ${esc(o.customerName)}</strong><small>${esc(o.status)} · ${esc(o.createdAt)}</small></div></div><div class="dispatch-action"><select class="form-select form-select-sm" aria-label="Driver for order ${o.id}" data-driver-for="${o.id}"><option value="">Select driver</option>${options}</select><button class="btn btn-sm btn-primary" data-assign="${o.id}" ${options?"":"disabled"}>Assign</button></div></div>`).join(""):'<p class="text-secondary mb-0">No unassigned deliveries.</p>';
        element("dispatchBatchDriver").innerHTML=`<option value="">Select driver</option>${options}`;
        if(selectedDriver)element("dispatchBatchDriver").value=selectedDriver;
        updateBatchButton();
        element("dispatchAssignments").innerHTML=data.assignments.length?data.assignments.map(a=>{const d=data.drivers.find(p=>p.id===a.driverId);return `<div class="dispatch-row"><div><strong>#${a.order.id} · ${esc(a.order.customerName)}</strong><small>${esc(a.state.replaceAll("_"," "))} · ${esc(d?d.firstName+" "+d.lastName:"Driver "+a.driverId)} · ${money(a.total)}</small></div>${a.state==="ASSIGNED"?(a.order.status==="Rejected"?`<button class="btn btn-sm btn-outline-danger" data-cancel-rejected="${a.order.id}">Cancel rejected order</button>`:`<div class="dispatch-action"><select class="form-select form-select-sm" aria-label="Reassign order ${a.order.id}" data-driver-for="${a.order.id}"><option value="">Reassign to</option>${options}</select><button class="btn btn-sm btn-outline-primary" data-assign="${a.order.id}">Reassign</button></div>`):""}</div>`;}).join(""):'<p class="text-secondary mb-0">Nothing assigned.</p>';
        element("dispatchRoster").innerHTML=data.drivers.length?data.drivers.map(d=>{const shift=data.shifts.find(s=>s.driverId===d.id);return `<div class="dispatch-row"><div><strong>${esc(d.firstName)} ${esc(d.lastName)}</strong><small>${esc(d.email)} · ${shift?esc(shift.state):"Off shift"}${shift?.returningAt?" · Returning":""}</small></div><div class="dispatch-action"><label>Hourly <input type="number" min="0" step="0.01" class="form-control form-control-sm" data-hourly="${d.id}" value="${esc(d.hourlyRate)}"></label><label>Delivery <input type="number" min="0" step="0.01" class="form-control form-control-sm" data-delivery="${d.id}" value="${esc(d.perDeliveryRate)}"></label><button class="btn btn-sm btn-outline-primary" data-rates="${d.id}" title="Save rates"><i class="bi bi-floppy"></i></button>${shift?.state==="CLOCKED_OUT"?`<button class="btn btn-sm btn-primary" data-cashup="${shift.id}">Cash-up</button>`:""}</div></div>`;}).join(""):'<p class="text-secondary mb-0">No drivers in this branch.</p>';
    }
    function updateBatchButton(){element("dispatchBatchAssign").disabled=!element("dispatchBatchDriver").value||!document.querySelector('[data-batch-order]:checked');}
    async function refresh() {try {render(await request(`/api/admin/dispatch?branchId=${branch()}`));error("");await history();}catch(e){error(e.message);}}
    async function history(){const rows=await request(`/api/admin/dispatch/settlements?branchId=${branch()}`);element("dispatchHistory").innerHTML=rows.length?rows.map(s=>`<div class="dispatch-row"><div><strong>Shift #${s.shiftId} · ${esc(s.managerUsername)}</strong><small>${esc(s.settledAt)} · ${money(s.orderSales)} sales · ${money(s.expectedCashDue)} expected · ${money(s.variance)} variance</small></div></div>`).join(""):'<p class="text-secondary mb-0">No settled shifts yet.</p>';}
    async function mutate(path,method,body){try{await request(path,method,body);error("");await refresh();}catch(e){error(e.message);}}
    function closeCashUp(){previewRevision++;cashUpShift=null;cashPreview=null;element("dispatchCashUp").classList.add("d-none");}
    function renderCashPreview(preview){
        cashPreview=preview;
        const values={dispatchDeliveredCount:preview.deliveredCount,dispatchOrderSales:money(preview.orderSales),
            dispatchOnlinePaid:money(preview.onlineOrderPaid),dispatchHourlyPay:money(preview.hourlyPay),
            dispatchDeliveryPay:money(preview.deliveryPay),dispatchOnlineTips:money(preview.onlineTips),
            dispatchCashTips:money(preview.cashTips),dispatchExpectedCash:money(preview.expectedCashDue)};
        Object.entries(values).forEach(([id,value])=>{element(id).textContent=value;});
        updateVariance();
    }
    function updateVariance(){
        if(!cashPreview)return;
        const handed=Number(element("dispatchCashHanded").value);
        const paid=Number(element("dispatchCashPaid").value);
        const variance=handed-paid-Number(cashPreview.expectedCashDue);
        element("dispatchVariance").textContent=money(variance);
        element("dispatchVarianceReason").required=Math.abs(variance)>0.005;
    }
    async function updateCashPreview(){
        if(cashUpShift===null)return;
        const minutes=Number(element("dispatchApprovedMinutes").value);
        if(!Number.isInteger(minutes)||minutes<0||minutes>1440)return;
        const revision=++previewRevision;
        cashPreview=null;
        element("dispatchCashUpSubmit").disabled=true;
        try{
            const preview=await request(`/api/admin/dispatch/shifts/${cashUpShift}/cash-up?branchId=${branch()}&approvedMinutes=${minutes}`);
            if(revision===previewRevision){renderCashPreview(preview);element("dispatchCashUpSubmit").disabled=false;}
        }catch(e){error(e.message);}
    }
    async function cashUp(shiftId){
        try{
            const preview=await request(`/api/admin/dispatch/shifts/${shiftId}/cash-up?branchId=${branch()}`);
            cashUpShift=Number(shiftId);
            previewRevision++;
            element("dispatchCashUpSubmit").disabled=false;
            element("dispatchCashUpHeading").textContent=`Cash-up · Shift #${shiftId}`;
            element("dispatchApprovedMinutes").value=String(preview.approvedMinutes);
            element("dispatchCashHanded").value=Math.max(0,Number(preview.expectedCashDue)).toFixed(2);
            element("dispatchCashPaid").value=Math.max(0,-Number(preview.expectedCashDue)).toFixed(2);
            element("dispatchVarianceReason").value="";
            renderCashPreview(preview);
            element("dispatchCashUp").classList.remove("d-none");
            element("dispatchCashUp").scrollIntoView({behavior:"smooth",block:"start"});
            error("");
        }catch(e){error(e.message);}
    }
    async function settleCashUp(event){
        event.preventDefault();
        if(cashUpShift===null||!cashPreview)return;
        const approvedMinutes=Number(element("dispatchApprovedMinutes").value);
        const cashHandedIn=Number(element("dispatchCashHanded").value);
        const cashPaidOut=Number(element("dispatchCashPaid").value);
        const varianceReason=element("dispatchVarianceReason").value.trim();
        if(!Number.isInteger(approvedMinutes)||approvedMinutes<0||approvedMinutes>1440
            ||!Number.isFinite(cashHandedIn)||cashHandedIn<0||!Number.isFinite(cashPaidOut)||cashPaidOut<0
            ||(cashHandedIn>0&&cashPaidOut>0)){
            error("Review the approved minutes and cash amounts.");return;
        }
        if(Math.abs(cashHandedIn-cashPaidOut-Number(cashPreview.expectedCashDue))>0.005&&!varianceReason){
            error("A variance reason is required.");return;
        }
        if(!confirm(`Settle shift #${cashUpShift}? This cannot be changed.`))return;
        try{
            await request(`/api/admin/dispatch/shifts/${cashUpShift}/cash-up`,"POST",
                {branchId:branch(),approvedMinutes,cashHandedIn,cashPaidOut,varianceReason});
            closeCashUp();error("");await refresh();
        }catch(e){error(e.message);}
    }
    function connect(){if(typeof createSimpleStompClient!=="function")return;liveClient=createSimpleStompClient({onConnect:()=>{delay=1000;const topic=fixedBranch?`/topic/admin/branches/${fixedBranch}/dispatch`:"/topic/admin/dispatch";liveClient.subscribe(topic,frame=>{try{const event=JSON.parse(frame.body);if(event.eventType==="RETURNING"&&event.branchId===branch()){const box=element("dispatchAlert");box.textContent=`${event.driverUsername} is returning to the store.`;box.classList.remove("d-none");}refresh();}catch(e){refresh();}});refresh();},onDisconnect:()=>{setTimeout(connect,delay);delay=Math.min(delay*2,15000);}});liveClient.connect();}
    document.addEventListener("DOMContentLoaded",()=>{
        element("dispatchRefresh").addEventListener("click",refresh);
        element("dispatchBatchDriver").addEventListener("change",updateBatchButton);
        element("dispatchQueue").addEventListener("change",updateBatchButton);
        element("dispatchBatchAssign").addEventListener("click",()=>{
            const orderIds=Array.from(document.querySelectorAll('[data-batch-order]:checked')).map(input=>Number(input.value));
            const driverId=Number(element("dispatchBatchDriver").value);
            if(!orderIds.length||!driverId)return;
            mutate("/api/admin/dispatch/assignments/batch","POST",{branchId:branch(),orderIds,driverId});
        });
        element("dispatchBranch")?.addEventListener("change",()=>{element("dispatchAlert").classList.add("d-none");closeCashUp();refresh();});
        element("dispatchApprovedMinutes").addEventListener("change",updateCashPreview);
        element("dispatchCashHanded").addEventListener("input",updateVariance);
        element("dispatchCashPaid").addEventListener("input",updateVariance);
        element("dispatchCashUpForm").addEventListener("submit",settleCashUp);
        element("dispatchCashUpClose").addEventListener("click",closeCashUp);
        element("dispatchCashUpCancel").addEventListener("click",closeCashUp);
        element("dispatchPromote").addEventListener("submit",event=>{event.preventDefault();mutate("/api/admin/dispatch/drivers","POST",{email:element("dispatchEmail").value,branchId:branch()});});
        element("dispatch").addEventListener("click",event=>{const button=event.target.closest("button[data-assign],button[data-rates],button[data-cashup],button[data-cancel-rejected]");if(!button)return;
            if(button.dataset.assign){const orderId=Number(button.dataset.assign);const driverId=Number(element("dispatch").querySelector(`select[data-driver-for="${orderId}"]:focus`)?.value||button.parentElement.querySelector("select")?.value);if(!driverId){error("Select an active driver first.");return;}mutate("/api/admin/dispatch/assignments","POST",{branchId:branch(),orderId,driverId});}
            if(button.dataset.rates){const id=button.dataset.rates;mutate(`/api/admin/dispatch/drivers/${id}/rates`,"PUT",{branchId:branch(),hourlyRate:button.parentElement.querySelector(`[data-hourly="${id}"]`).value,perDeliveryRate:button.parentElement.querySelector(`[data-delivery="${id}"]`).value});}
            if(button.dataset.cashup)cashUp(button.dataset.cashup);
            if(button.dataset.cancelRejected)mutate(`/api/admin/dispatch/assignments/${button.dataset.cancelRejected}/cancel-rejected?branchId=${branch()}`,"POST");
        });
        refresh();connect();
    });
})();
