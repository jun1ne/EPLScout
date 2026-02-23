console.log("TEAM DETAIL JS LOADED");

let positionChartInstance = null;

document.addEventListener("DOMContentLoaded", async () => {

    const params = new URLSearchParams(location.search);
    const teamId = params.get("teamId");
    let season = params.get("season") || 2025;

    if (!teamId) return;

    const seasonSelect = document.getElementById("seasonSelect");

    if (seasonSelect) {
        seasonSelect.value = season;

        seasonSelect.addEventListener("change", async () => {
            const newSeason = seasonSelect.value;

            const newUrl =
                `/team.html?teamId=${teamId}&season=${newSeason}`;
            window.history.pushState({}, "", newUrl);

            await loadTeamSummary(teamId, newSeason);
            await loadPlayers(teamId, newSeason);
        });
    }

    await loadTeamSummary(teamId, season);
    await loadPlayers(teamId, season);
});


/* ===============================
   팀 요약 로드
================================ */
async function loadTeamSummary(teamId, season) {

    try {

        const res = await fetch(`/api/team/summary/${teamId}/${season}`);
        const data = await res.json();

        document.getElementById("teamName").innerText =
            data.teamName ?? "팀 정보";

        const logoEl = document.getElementById("teamLogo");

        if (logoEl) {
            if (data.teamLogo) {
                logoEl.src = data.teamLogo;
                logoEl.style.display = "block";
            } else {
                logoEl.style.display = "none";
            }
        }

        document.getElementById("avgAgeStat").innerText =
            data.avgAge ? data.avgAge.toFixed(1) : "-";

        document.getElementById("avgRatingStat").innerText =
            data.avgRating ? data.avgRating.toFixed(2) : "-";

        document.getElementById("seasonStat").innerText = season;

        const weakBox = document.getElementById("weakPositionsBox");
        const countBox = document.getElementById("positionCountBox");

        if (weakBox && countBox) {

            const weakPositions = data.weakPositions || [];
            const positionCounts = data.positionCounts || {};

            weakBox.innerHTML =
                weakPositions.length > 0
                    ? `<strong>약점 포지션:</strong> ${weakPositions.map(convertPosition).join(", ")}`
                    : `<strong>약점 포지션:</strong> 없음`;

            let countHtml = "<strong>포지션별 인원:</strong><br>";

            for (const [pos, cnt] of Object.entries(positionCounts)) {
                countHtml += `${convertPosition(pos)}: ${cnt}명<br>`;
            }

            countBox.innerHTML = countHtml;
        }

        if (data.positionAvgRatings) {
            renderPositionChart(data.positionAvgRatings);
        }

    } catch (err) {
        console.error("팀 요약 로딩 실패", err);
    }
}


/* ===============================
   포지션별 평균 평점 차트
================================ */
function renderPositionChart(positionAvgRatings) {

    const canvas = document.getElementById("positionChart");
    if (!canvas) return;

    if (positionChartInstance) {
        positionChartInstance.destroy();
    }

    const labels = Object.keys(positionAvgRatings || {}).map(convertPosition);
    const values = Object.values(positionAvgRatings || {});

    positionChartInstance = new Chart(canvas, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: '평균 평점',
                data: values
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
    });
}


/* ===============================
   선수 목록 로드
================================ */
async function loadPlayers(teamId, season) {

    try {

        const res =
            await fetch(`/api/team/${teamId}/players?season=${season}`);
        const players = await res.json();

        const body = document.getElementById("playerTableBody");
        body.innerHTML = "";

        players.forEach(p => {

            const shirt =
                (p.shirtNumber && p.shirtNumber !== 0)
                    ? p.shirtNumber
                    : "-";

            const posClass =
                p.position
                    ? `pos-${p.position.toLowerCase()}`
                    : "";

            const tr = document.createElement("tr");

            // =========================
            // 부상 뱃지 생성
            // =========================
            let injuryBadge = "";
            if (p.isInjured) {

                let tooltip = "부상 중";

                if (p.injuryType) {
                    tooltip += ` - ${p.injuryType}`;
                }

                if (p.expectedReturn) {
                    tooltip += ` (복귀 예상: ${p.expectedReturn})`;
                }

                injuryBadge =
                    `<span class="injury-badge" title="${tooltip}">
                        🔴 부상
                     </span>`;
            }

            tr.innerHTML = `
                <td>${shirt}</td>

                <td class="player-cell">
                    <img src="${p.photoUrl ?? ''}"
                         class="player-photo"
                         onerror="this.style.display='none'"/>
                    ${p.name}
                    ${injuryBadge}
                </td>

                <td>
                    <span class="pos-badge ${posClass}">
                        ${convertPosition(p.position) ?? "-"}
                    </span>
                </td>

                <td>${p.age ?? "-"}</td>

                <td>${p.nationality ?? "-"}</td>
            `;

            tr.style.cursor = "pointer";

            tr.addEventListener("click", () => {
                window.location.href =
                    `/player.html?playerId=${p.playerId}&season=${season}`;
            });

            body.appendChild(tr);
        });

        document.getElementById("squadSizeStat").innerText =
            players.length;

    } catch (err) {
        console.error("선수 목록 로딩 실패", err);
    }
}


/* ===============================
   포지션 한글 변환
================================ */
function convertPosition(pos) {
    return {
        Goalkeeper: "골키퍼",
        Defender: "수비수",
        Midfielder: "미드필더",
        Attacker: "공격수",
        GK: "골키퍼",
        DF: "수비수",
        MF: "미드필더",
        FW: "공격수"
    }[pos] || pos;
}
