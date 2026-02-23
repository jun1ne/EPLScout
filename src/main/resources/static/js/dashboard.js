console.log("DASHBOARD JS LOADED");

let leagueData = [];
let chartInstance = null;

document.addEventListener("DOMContentLoaded", () => {
    bindSeasonSelector();
    loadDashboard();
    bindChartMenu();
});

/* ===============================
   시즌 선택 이벤트
================================ */
function bindSeasonSelector() {
    const seasonSelect = document.getElementById("seasonSelect");
    if (!seasonSelect) return;

    seasonSelect.addEventListener("change", () => {
        loadDashboard();
    });
}

function getSelectedSeason() {
    const seasonSelect = document.getElementById("seasonSelect");
    return seasonSelect ? seasonSelect.value : 2023;
}

/* ===============================
   공통 차트 옵션
================================ */
const commonChartOptions = {
    responsive: true,
    indexAxis: "y",
    plugins: {
        legend: {
            position: "top",
            labels: { boxWidth: 14, font: { size: 13 } }
        }
    }
};

/* ===============================
   대시보드 로드
================================ */
async function loadDashboard() {

    const leagueId = 39;
    const season = getSelectedSeason();

    try {
        const res = await fetch(
            `/api/league/standings?leagueId=${leagueId}&season=${season}`
        );

        leagueData = await res.json();

        renderLeagueStandings(leagueData);
        renderGoalsChart();
        renderRecentMatches();
        renderLeagueNews();

        const seasonTitle = document.getElementById("seasonTitle");
        if (seasonTitle) {
            seasonTitle.innerText = `${season} Season`;
        }

    } catch (e) {
        console.error("대시보드 로드 실패:", e);
    }
}

/* ===============================
   순위표 렌더링 + 팀 클릭 이동
================================ */
function renderLeagueStandings(list) {

    const body = document.getElementById("leagueStandings");
    body.innerHTML = "";

    list.forEach(row => {

        const tr = document.createElement("tr");

        if (row.rank <= 4) tr.classList.add("top4");
        if (row.rank >= 18) tr.classList.add("relegation");

        const logoHtml = row.teamLogo
            ? `<img src="${row.teamLogo}" class="team-logo">`
            : "";

        tr.innerHTML = `
            <td>${row.rank}</td>
            <td>
                <div class="team-cell">
                    ${logoHtml}
                    <span>${row.teamName}</span>
                </div>
            </td>
            <td>${row.played}</td>
            <td>${row.win}</td>
            <td>${row.draw}</td>
            <td>${row.lose}</td>
            <td class="points">${row.points}</td>
            <td>${row.goalDiff}</td>
        `;

        //  팀 상세 이동
        tr.style.cursor = "pointer";
        tr.addEventListener("click", () => {
            location.href = `/team.html?teamId=${row.teamId}&season=${getSelectedSeason()}`;
        });

        body.appendChild(tr);
    });
}

/* ===============================
   차트 메뉴
================================ */
function bindChartMenu() {
    document.querySelectorAll(".chart-btn").forEach(btn => {
        btn.addEventListener("click", () => {

            document.querySelectorAll(".chart-btn")
                .forEach(b => b.classList.remove("active"));

            btn.classList.add("active");

            const type = btn.dataset.chart;

            if (type === "goals") renderGoalsChart();
            if (type === "results") renderResultsChart();
            if (type === "goalDiff") renderGoalDiffChart();
        });
    });
}

function resetChart() {
    if (chartInstance) chartInstance.destroy();
}

/* ===============================
   차트들
================================ */
function renderGoalsChart() {

    resetChart();

    const sorted = [...leagueData]
        .sort((a, b) => b.goalsFor - a.goalsFor)
        .slice(0, 10);

    chartInstance = new Chart(
        document.getElementById("leagueChart"),
        {
            type: "bar",
            data: {
                labels: sorted.map(t => t.teamName),
                datasets: [
                    { label: "득점", data: sorted.map(t => t.goalsFor) },
                    { label: "실점", data: sorted.map(t => t.goalsAgainst) }
                ]
            },
            options: commonChartOptions
        }
    );
}

function renderResultsChart() {

    resetChart();

    const sorted = [...leagueData]
        .sort((a, b) => b.win - a.win)
        .slice(0, 10);

    chartInstance = new Chart(
        document.getElementById("leagueChart"),
        {
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
                scales: { x: { stacked: true }, y: { stacked: true } }
            }
        }
    );
}

function renderGoalDiffChart() {

    resetChart();

    const sorted = [...leagueData]
        .sort((a, b) => b.goalDiff - a.goalDiff)
        .slice(0, 10);

    chartInstance = new Chart(
        document.getElementById("leagueChart"),
        {
            type: "bar",
            data: {
                labels: sorted.map(t => t.teamName),
                datasets: [
                    { label: "득실차", data: sorted.map(t => t.goalDiff) }
                ]
            },
            options: commonChartOptions
        }
    );
}

/* ===============================
   최근 경기 
================================ */
function renderRecentMatches() {

    const container = document.getElementById("recentMatches");
    if (!container) return;

    const matches = [
        {
            home: "Arsenal",
            away: "Chelsea",
            homeLogo: "https://media.api-sports.io/football/teams/42.png",
            awayLogo: "https://media.api-sports.io/football/teams/49.png",
            score: "2 - 1",
            status: "FT"
        },
        {
            home: "Man City",
            away: "Wolves",
            homeLogo: "https://media.api-sports.io/football/teams/50.png",
            awayLogo: "https://media.api-sports.io/football/teams/39.png",
            score: "3 - 0",
            status: "FT"
        }
    ];

    container.innerHTML = "";

    matches.forEach(match => {

        const div = document.createElement("div");
        div.className = "match-card-modern";

        div.innerHTML = `
            <div class="team">
                <img src="${match.homeLogo}">
                <span>${match.home}</span>
            </div>

            <div class="score">
                ${match.score}
                <span class="match-status">${match.status}</span>
            </div>

            <div class="team">
                <img src="${match.awayLogo}">
                <span>${match.away}</span>
            </div>
        `;

        container.appendChild(div);
    });
}

/* ===============================
   리그 뉴스
================================ */
function renderLeagueNews() {

    const container = document.getElementById("leagueNews");
    if (!container) return;

    const newsList = [
        {
            title: "Arsenal title race intensifies",
            tag: "Premier League",
            time: "2시간 전"
        },
        {
            title: "Manchester City tactical shift explained",
            tag: "Analysis",
            time: "5시간 전"
        }
    ];

    container.innerHTML = "";

    newsList.forEach(news => {

        const div = document.createElement("div");
        div.className = "news-card-modern";

        div.innerHTML = `
            <div class="news-title">${news.title}</div>
            <div class="news-meta">${news.tag} • ${news.time}</div>
        `;

        container.appendChild(div);
    });
}
