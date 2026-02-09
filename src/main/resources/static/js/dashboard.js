console.log("DASHBOARD JS LOADED");

let leagueData = [];
let chartInstance = null;

document.addEventListener("DOMContentLoaded", () => {
    loadDashboard();
    bindChartMenu();
});

/* ===============================
   공통 차트 옵션 (가독성 강화)
================================ */
const commonChartOptions = {
    responsive: true,
    indexAxis: "y", // 🔥 가로 차트 (팀 이름 가독성 핵심)
    plugins: {
        legend: {
            position: "top",
            labels: {
                boxWidth: 14,
                font: { size: 13 }
            }
        },
        tooltip: {
            callbacks: {
                label: ctx =>
                    `${ctx.dataset.label}: ${ctx.formattedValue}`
            }
        }
    },
    scales: {
        x: {
            ticks: {
                font: { size: 12 }
            }
        },
        y: {
            ticks: {
                font: { size: 13 }
            }
        }
    }
};

/* ===============================
   차트 설명 텍스트 변경
================================ */
function setChartDesc(type) {
    const desc = document.getElementById("chartDesc");
    if (!desc) return;

    const map = {
        goals: "득점 기준으로 팀을 정렬하여 득점·실점을 비교합니다.",
        results: "승리 횟수 기준으로 팀 성과를 비교합니다.",
        goalDiff: "득실차 기준으로 공격 효율을 비교합니다."
    };

    desc.innerText = map[type];
}

/* ===============================
   대시보드 데이터 로드
================================ */
async function loadDashboard() {
    const leagueId = 39;
    const season = 2023;

    try {
        const res = await fetch(
            `/api/league/standings?leagueId=${leagueId}&season=${season}`
        );

        leagueData = await res.json();

        renderLeagueStandings(leagueData);

        // 초기 차트
        setChartDesc("goals");
        renderGoalsChart();

    } catch (e) {
        console.error(e);
    }
}

/* ===============================
   리그 순위표 (기존 그대로)
================================ */
function renderLeagueStandings(list) {
    const body = document.getElementById("leagueStandings");
    body.innerHTML = "";

    list.forEach(row => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${row.rank}</td>
            <td>${row.teamName}</td>
            <td>${row.played}</td>
            <td>${row.win}</td>
            <td>${row.draw}</td>
            <td>${row.lose}</td>
            <td><strong>${row.points}</strong></td>
            <td>${row.goalDiff}</td>
        `;
        body.appendChild(tr);
    });
}

/* ===============================
   차트 메뉴 이벤트
================================ */
function bindChartMenu() {
    document.querySelectorAll(".chart-btn").forEach(btn => {
        btn.addEventListener("click", () => {

            document
                .querySelectorAll(".chart-btn")
                .forEach(b => b.classList.remove("active"));

            btn.classList.add("active");

            const type = btn.dataset.chart;
            setChartDesc(type);

            switch (type) {
                case "goals":
                    renderGoalsChart();
                    break;
                case "results":
                    renderResultsChart();
                    break;
                case "goalDiff":
                    renderGoalDiffChart();
                    break;
            }
        });
    });
}

/* ===============================
   차트 초기화
================================ */
function resetChart() {
    if (chartInstance) {
        chartInstance.destroy();
    }
}

/* ===============================
   1. 득점 / 실점 차트
   - 득점 많은 순 정렬
================================ */
function renderGoalsChart() {
    resetChart();

    const ctx = document.getElementById("leagueChart");

    const sorted = [...leagueData]
        .sort((a, b) => b.goalsFor - a.goalsFor)
        .slice(0, 10);

    chartInstance = new Chart(ctx, {
        type: "bar",
        data: {
            labels: sorted.map(t => t.teamName),
            datasets: [
                { label: "득점", data: sorted.map(t => t.goalsFor) },
                { label: "실점", data: sorted.map(t => t.goalsAgainst) }
            ]
        },
        options: commonChartOptions
    });
}

/* ===============================
   2. 승 / 무 / 패 차트
   - 승 많은 순 정렬
================================ */
function renderResultsChart() {
    resetChart();

    const ctx = document.getElementById("leagueChart");

    const sorted = [...leagueData]
        .sort((a, b) => b.win - a.win)
        .slice(0, 10);

    chartInstance = new Chart(ctx, {
        type: "bar",
        data: {
            labels: sorted.map(t => t.teamName),
            datasets: [
                { label: "승", data: sorted.map(t => t.win), stack: "r" },
                { label: "무", data: sorted.map(t => t.draw), stack: "r" },
                { label: "패", data: sorted.map(t => t.lose), stack: "r" }
            ]
        },
        options: {
            ...commonChartOptions,
            scales: {
                x: { stacked: true },
                y: { stacked: true }
            }
        }
    });
}

/* ===============================
   3. 득실차 차트
   - 득실차 높은 순 정렬
================================ */
function renderGoalDiffChart() {
    resetChart();

    const ctx = document.getElementById("leagueChart");

    const sorted = [...leagueData]
        .sort((a, b) => b.goalDiff - a.goalDiff)
        .slice(0, 10);

    chartInstance = new Chart(ctx, {
        type: "bar",
        data: {
            labels: sorted.map(t => t.teamName),
            datasets: [
                { label: "득실차", data: sorted.map(t => t.goalDiff) }
            ]
        },
        options: commonChartOptions
    });
}
