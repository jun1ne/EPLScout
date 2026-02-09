console.log("TEAM JS LOADED");

let playerChart = null;

document.addEventListener("DOMContentLoaded", async () => {
    await loadTeams();

    document.getElementById("loadBtn")
        .addEventListener("click", loadTeamSummary);

    document.getElementById("goRecommendBtn")
        .addEventListener("click", goToRecommend);
});

/* ===============================
   팀 목록 로드
================================ */
async function loadTeams() {
    const season = document.getElementById("seasonInput").value;
    const select = document.getElementById("teamSelect");

    try {
        const res = await fetch(`/api/team/list?season=${season}`);
        const teams = await res.json();

        select.innerHTML = `<option value="">팀 선택</option>`;
        teams.forEach(team => {
            const opt = document.createElement("option");
            opt.value = team.teamId;
            opt.textContent = team.name;
            select.appendChild(opt);
        });
    } catch (e) {
        console.error(e);
        select.innerHTML = `<option value="">팀 로드 실패</option>`;
    }
}

/* ===============================
   팀 요약 + 선수단 조회
================================ */
async function loadTeamSummary() {
    const teamId = document.getElementById("teamSelect").value;
    const season = document.getElementById("seasonInput").value;

    if (!teamId) {
        alert("팀을 선택하세요");
        return;
    }

    try {
        const res = await fetch(`/api/team/summary/${teamId}/${season}`);
        const data = await res.json();
        renderTeamDetail(data);
        loadPlayers(teamId, season);
    } catch (e) {
        console.error(e);
    }
}

/* ===============================
   팀 상세 정보
================================ */
function renderTeamDetail(data) {
    let html = `
        <p><strong>평균 연령:</strong> ${data.avgAge.toFixed(1)}</p>
        <p><strong>평균 평점:</strong> ${data.avgRating.toFixed(2)}</p>

        <h4>포지션별 현황</h4>
        <ul>
    `;

    for (const pos in data.positionCounts) {
        html += `<li>${displayPositionName(pos)}: ${data.positionCounts[pos]}명</li>`;
    }

    html += `
        </ul>
        <hr>

        <h3>선수단</h3>
        <ul id="playerList">로딩 중...</ul>

        <div id="playerChartArea" style="display:none;">
            <h3 id="playerName"></h3>
            <canvas id="playerChart"></canvas>
        </div>
    `;

    document.getElementById("teamDetail").innerHTML = html;
}

/* ===============================
   선수 목록
================================ */
async function loadPlayers(teamId, season) {
    const list = document.getElementById("playerList");

    try {
        const res = await fetch(`/api/team/${teamId}/players?season=${season}`);
        const players = await res.json();

        list.innerHTML = "";

        players.forEach(p => {
            const li = document.createElement("li");
            li.style.cursor = "pointer";
            li.innerText = `${p.name} (${displayPositionName(p.position)})`;
            li.onclick = () => loadPlayerDetail(p.playerId);
            list.appendChild(li);
        });

    } catch (e) {
        console.error(e);
        list.innerHTML = "<li>선수 로드 실패</li>";
    }
}

/* ===============================
   선수 상세 조회
================================ */
async function loadPlayerDetail(playerId) {
    const season = document.getElementById("seasonInput").value;

    try {
        const res = await fetch(`/api/team/player/${playerId}?season=${season}`);
        const player = await res.json();

        console.log("차트용 선수 데이터:", player);

        renderPlayerChart(player);

    } catch (e) {
        console.error("선수 상세 조회 실패", e);
    }
}

/* ===============================
   포지션 정규화 
================================ */
function normalizePosition(pos) {
    return {
        FW: "FW",
        Attacker: "FW",
        Forward: "FW",

        MF: "MF",
        Midfielder: "MF",

        DF: "DF",
        Defender: "DF",

        GK: "GK",
        Goalkeeper: "GK"
    }[pos] || pos;
}

/* ===============================
   차트 렌더링
================================ */
function renderPlayerChart(player) {
    const area = document.getElementById("playerChartArea");
    const title = document.getElementById("playerName");
    const ctx = document.getElementById("playerChart");

    area.style.display = "block";
    title.innerText = `${player.name} (${displayPositionName(player.position)})`;

    if (playerChart) {
        playerChart.destroy();
    }

    const config = getChartConfigByPosition(player);

    playerChart = new Chart(ctx, {
        type: "bar",
        data: {
            labels: config.labels,
            datasets: [{
                label: config.label,
                data: config.data
            }]
        },
        options: {
            responsive: true,
            scales: {
                y: { beginAtZero: true }
            }
        }
    });
}

/* ===============================
   포지션별 지표
================================ */
function getChartConfigByPosition(p) {

    const pos = normalizePosition(p.position);
    const v = x => x ?? 0;

    switch (pos) {

        case "FW":
            return {
                label: "공격 지표",
                labels: ["득점", "어시스트", "슈팅"],
                data: [v(p.goals), v(p.assists), v(p.shots)]
            };

        case "MF":
            return {
                label: "미드필더 지표",
                labels: ["어시스트", "키패스", "패스 성공률"],
                data: [v(p.assists), v(p.keyPasses), v(p.passAccuracy)]
            };

        case "DF":
            return {
                label: "수비 지표",
                labels: ["태클", "인터셉트", "클리어"],
                data: [v(p.tackles), v(p.interceptions), v(p.clearances)]
            };

        case "GK":
            return {
                label: "골키퍼 지표",
                labels: ["세이브", "실점"],
                data: [v(p.saves), v(p.goalsConceded)]
            };

        default:
            console.warn("알 수 없는 포지션:", p.position);
            return {
                label: "기본 지표",
                labels: ["출전", "평점"],
                data: [v(p.appearances), v(p.avgRating)]
            };
    }
}

/* ===============================
   기타
================================ */
function goToRecommend() {
    const teamId = document.getElementById("teamSelect").value;
    const season = document.getElementById("seasonInput").value;
    location.href = `/index.html?teamId=${teamId}&season=${season}`;
}

function displayPositionName(pos) {
    return {
        GK: "Goalkeeper",
        DF: "Defender",
        MF: "Midfielder",
        FW: "Attacker"
    }[pos] || pos;
}
