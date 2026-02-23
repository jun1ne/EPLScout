let chartInstance = null;
let trendChartInstance = null;

document.addEventListener("DOMContentLoaded", async () => {

    const params = new URLSearchParams(location.search);
    const playerId = params.get("playerId");
    const season = params.get("season") || 2025;

    if (!playerId) return;

    /* ===============================
       1. 단일 시즌 데이터
    =============================== */
    const res =
        await fetch(`/api/player/${playerId}?season=${season}`);
    const player = await res.json();

    /* ===============================
       2. 리그 평균
    =============================== */
    const leagueRes =
        await fetch(`/api/player/${playerId}/league-average?season=${season}`);
    const leagueData = await leagueRes.json();

    renderProfile(player);
    renderRadarChart(player, leagueData);

    /* ===============================
       3. 시즌별 평점 추세
    =============================== */
    const trendRes =
        await fetch(`/api/player/${playerId}/trend`);
    const trendData = await trendRes.json();

    renderTrendChart(trendData);

    /* ===============================
       4. 선수 가치 점수
    =============================== */
    const valueRes =
        await fetch(`/api/player/${playerId}/value?season=${season}`);
    const valueData = await valueRes.json();

    renderValue(valueData);

    /* ===============================
       5. 리그 비교 + Scout Index
    =============================== */
    renderLeagueCompare(player, leagueData);
    renderPerformanceSummary(valueData, trendData);
});


/* ===============================
   왼쪽 프로필 카드
================================ */
function renderProfile(p) {

    document.getElementById("playerProfile").innerHTML = `
        <div class="player-card">

            <div class="player-header">
                <img src="${p.photoUrl ?? ""}" 
                     class="player-photo-large"
                     onerror="this.style.display='none'">

                <div class="player-basic-info">
                    <h2>${p.name}</h2>
                    <p class="player-position">${p.position}</p>
                    <p>Age: ${p.age ?? "-"}</p>
                </div>
            </div>

            <div class="player-stats-grid">

                <div class="stat-box">
                    <span>Appearances</span>
                    <strong>${p.appearances ?? 0}</strong>
                </div>

                <div class="stat-box">
                    <span>Goals</span>
                    <strong>${p.goals ?? 0}</strong>
                </div>

                <div class="stat-box">
                    <span>Assists</span>
                    <strong>${p.assists ?? 0}</strong>
                </div>

                <div class="stat-box">
                    <span>Avg Rating</span>
                    <strong>${p.avgRating ? p.avgRating.toFixed(2) : "-"}</strong>
                </div>

                <div class="stat-box">
                    <span>Pass Accuracy</span>
                    <strong>${p.passAccuracy ?? 0}%</strong>
                </div>

            </div>

            <div class="trend-container">
                <canvas id="trendChart"></canvas>
            </div>

        </div>
    `;
}


/* ===============================
  Radar Chart
   - 0~100 정규화
   - 리그 평균 Overlay
   - Grid 미니멀화
================================ */
function renderRadarChart(p, leagueAvg) {

    if (chartInstance) chartInstance.destroy();

    const labels = [
        "Goals",
        "Assists",
        "Shots",
        "Key Passes",
        "Rating"
    ];

    const normalize = (value, max) =>
        Math.min((value / max) * 100, 120);

    const playerValues = [
        normalize(p.goals ?? 0, 30),
        normalize(p.assists ?? 0, 20),
        normalize(p.shots ?? 0, 100),
        normalize(p.keyPasses ?? 0, 80),
        normalize(p.avgRating ?? 0, 10)
    ];

    let datasets = [];

    //  League Avg 먼저 그려서 뒤로 보내기
    if (leagueAvg) {

        const leagueValues = [
            normalize(leagueAvg.avgGoals ?? 0, 30),
            normalize(leagueAvg.avgAssists ?? 0, 20),
            normalize(leagueAvg.avgShots ?? 0, 100),
            normalize(leagueAvg.avgKeyPasses ?? 0, 80),
            normalize(leagueAvg.avgRating ?? 0, 10)
        ];

        datasets.push({
            label: "League Avg",
            data: leagueValues,
            borderColor: "#bbbbbb",
            backgroundColor: "transparent",
            borderWidth: 2,
            borderDash: [6,6],  //  점선
            pointRadius: 2
        });
    }

    //  Player 강조
    datasets.push({
        label: "Player",
        data: playerValues,
        backgroundColor: "rgba(88,166,255,0.45)",
        borderColor: "#58a6ff",
        borderWidth: 3,
        pointBackgroundColor: "#58a6ff",
        pointRadius: 4
    });

    chartInstance = new Chart(
        document.getElementById("playerChart"),
        {
            type: "radar",
            data: {
                labels: labels,
                datasets: datasets
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        labels: { color: "#fff" }
                    }
                },
                scales: {
                    r: {
                        min: 0,
                        max: 120,  
                        ticks: {
                            stepSize: 30,
                            color: "#aaa",
                            backdropColor: "transparent"
                        },
                        grid: {
                            color: "rgba(255,255,255,0.08)"
                        },
                        angleLines: {
                            color: "rgba(255,255,255,0.08)"
                        },
                        pointLabels: {
                            color: "#ddd",
                            font: { size: 12 }
                        }
                    }
                }
            }
        }
    );
}



/* ===============================
   시즌별 평점 추세
================================ */
function renderTrendChart(data) {

    if (trendChartInstance) trendChartInstance.destroy();

    trendChartInstance = new Chart(
        document.getElementById("trendChart"),
        {
            type: "line",
            data: {
                labels: data.map(d => d.season),
                datasets: [{
                    label: "Rating Trend",
                    data: data.map(d => d.avgRating),
                    tension: 0.4,
                    borderColor: "#58a6ff",
                    backgroundColor: "rgba(88,166,255,0.2)",
                    fill: true
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        min: 5,
                        max: 10
                    }
                }
            }
        }
    );
}


/* ===============================
   Market Value 표시
================================ */
function renderValue(v) {

    const container =
        document.querySelector(".player-stats-grid");

    const div = document.createElement("div");
    div.className = "stat-box";

    div.innerHTML = `
        <span>Market Value</span>
        <strong>${v.valueScore.toFixed(1)} (${v.grade})</strong>
    `;

    container.appendChild(div);
}


/* ===============================
   리그 평균 비교
================================ */
function renderLeagueCompare(p, avg) {

    const container = document.getElementById("leagueCompare");
    if (!container) return;

    container.innerHTML = `
        <h4>League Comparison</h4>
        <p>Goals: ${p.goals ?? 0} (Avg ${avg.avgGoals?.toFixed(1) ?? "-"})</p>
        <p>Assists: ${p.assists ?? 0} (Avg ${avg.avgAssists?.toFixed(1) ?? "-"})</p>
        <p>Shots: ${p.shots ?? 0} (Avg ${avg.avgShots?.toFixed(1) ?? "-"})</p>
    `;
}


/* ===============================
   Scout Index 계산
================================ */
function renderPerformanceSummary(v, trendData) {

    const container = document.getElementById("performanceSummary");
    if (!container) return;

    let trendMomentum = 0;
    if (trendData.length >= 2) {
        const latest = trendData[trendData.length - 1].avgRating;
        const prev = trendData[trendData.length - 2].avgRating;
        trendMomentum = latest - prev;
    }

    const scoutIndex =
        (v.valueScore * 0.3) +
        (v.performanceScore ? v.performanceScore * 0.6 : 50 * 0.6) +
        (trendMomentum * 10 * 0.1);

    const gradeClass = getGradeClass(v.grade);

    container.innerHTML = `
        <div class="performance-box">
            <span>Value Score</span>
            <strong>${v.valueScore.toFixed(1)}</strong>
        </div>

        <div class="performance-box ${gradeClass}">
            <span>Grade</span>
            <strong>${v.grade}</strong>
        </div>

        <div class="performance-box">
            <span>Scout Index</span>
            <strong>${Math.round(scoutIndex)}</strong>
        </div>
    `;
}


/* ===============================
   등급 색상 매핑
================================ */
function getGradeClass(grade) {

    if (!grade) return "";

    if (grade.startsWith("S")) return "grade-s";
    if (grade.startsWith("A")) return "grade-a";
    if (grade.startsWith("B")) return "grade-b";
    if (grade.startsWith("C")) return "grade-c";
    return "grade-d";
}
