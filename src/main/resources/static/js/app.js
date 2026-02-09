console.log("APP JS LOADED");

document.addEventListener("DOMContentLoaded", async () => {
    await loadTeams();
    autoRunFromQuery();

    document
        .getElementById("recommendBtn")
        .addEventListener("click", recommendPlayers);
});

/* ===============================
   URL 파라미터 자동 실행
================================ */
function autoRunFromQuery() {
    const params = new URLSearchParams(location.search);
    const teamId = params.get("teamId");
    const season = params.get("season");

    if (!teamId) return;

    if (season) {
        document.getElementById("seasonInput").value = season;
    }

    setTimeout(() => {
        document.getElementById("teamSelect").value = teamId;
        recommendPlayers();
    }, 300);
}

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
   추천 실행
================================ */
async function recommendPlayers() {
    const teamId = document.getElementById("teamSelect").value;
    const season = document.getElementById("seasonInput").value;

    if (!teamId) {
        alert("팀을 선택하세요");
        return;
    }

    let summary;

    try {
        const res = await fetch(`/api/team/summary/${teamId}/${season}`);
        summary = await res.json();
        renderTeamAnalysis(summary);
    } catch (e) {
        console.error(e);
        document.getElementById("teamAnalysis").innerText =
            "팀 분석 데이터를 불러오지 못했습니다.";
        return;
    }

    try {
        const res = await fetch(`/api/scout/view/${teamId}/${season}`);
        const data = await res.json();
        console.log("추천 API 응답:", data); 
        renderRecommendList(data.recommendations, summary);
    } catch (e) {
        console.error(e);
        document.getElementById("recommendList").innerText =
            "추천 선수를 불러오지 못했습니다.";
    }
}

/* ===============================
   팀 분석 출력
================================ */
function renderTeamAnalysis(data) {
    let html = `
        <p><strong>평균 연령:</strong> ${data.avgAge.toFixed(1)}</p>
        <p><strong>평균 평점:</strong> ${data.avgRating.toFixed(2)}</p>

        <h4>포지션 분포</h4>
        <ul>
    `;

    for (const pos in data.positionCounts) {
        const count = data.positionCounts[pos];
        const avgRating = data.positionAvgRatings?.[pos];

        html += `
            <li>
                ${displayPositionName(pos)}: ${count}명
                ${avgRating ? `(평균 평점 ${avgRating.toFixed(2)})` : ""}
            </li>
        `;
    }

    html += `</ul>`;

    if (data.weakestPositionByRating) {
        html += `
            <p class="badge warning">
                평점 기준 약점 포지션:
                ${displayPositionName(data.weakestPositionByRating)}
            </p>`;
    }

    if (data.weakPositions && data.weakPositions.length > 0) {
        html += `
            <p class="badge danger">
                인원 부족 포지션:
                ${data.weakPositions.map(displayPositionName).join(", ")}
            </p>`;
    }

    document.getElementById("teamAnalysis").innerHTML = html;
}

/* ===============================
   추천 선수 렌더링 
================================ */
function renderRecommendList(players, summary) {
    const list = document.getElementById("recommendList");
    list.innerHTML = "";

    if (!players || players.length === 0) {
        list.innerHTML = "<p>추천 선수가 없습니다.</p>";
        return;
    }

    // 약점 포지션을 풀네임으로 변환
    const weakPosition =
        normalizePosition(summary?.weakestPositionByRating);

    players.forEach(player => {
        const score = Number(player.score ?? 0);
        const potential = Number(player.potentialScore ?? 0);

       
        const isWeak =
            weakPosition &&
            normalizePosition(player.position) === weakPosition;

        const card = document.createElement("div");
        card.className = "player-card";

        const hasBreakdown =
            player.baseScore !== undefined &&
            player.positionBonus !== undefined;

        card.innerHTML = `
            <div class="player-header">
                <div>
                    <strong>${player.playerName}</strong> (${player.age}세)<br>
                    ${displayPositionName(player.position)}
                    · 점수 ${score.toFixed(2)}
                    · 포텐셜 ${potential.toFixed(2)}
                    ${isWeak ? `<span class="badge warning">팀 약점 포지션</span>` : ""}
                </div>
                <button class="btn small toggle-btn">추천 사유 보기</button>
            </div>

            ${hasBreakdown ? `
            <div class="score-breakdown">
                <small>
                    점수 구성:
                    base ${Number(player.baseScore).toFixed(2)}
                    + 포지션 보정 ${Number(player.positionBonus).toFixed(2)}
                </small>
            </div>
            ` : ``}

            <div class="reason-box" style="display:none;"></div>
        `;

        const btn = card.querySelector(".toggle-btn");
        const box = card.querySelector(".reason-box");
        let loaded = false;

        btn.onclick = async () => {
            if (box.style.display === "block") {
                box.style.display = "none";
                btn.innerText = "추천 사유 보기";
                return;
            }

            box.style.display = "block";
            btn.innerText = "닫기";

            if (loaded) return;

            box.innerText = "추천 사유 생성 중...";

            try {
                const teamName =
                    document.getElementById("teamSelect")
                        .selectedOptions[0].textContent;

                const res = await fetch(
                    "/api/scout/recommendation/reason",
                    {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            teamName,
                            playerName: player.playerName,
                            position: player.position,
                            score,
                            potential
                        })
                    }
                );

                box.innerText = await res.text();
                loaded = true;
            } catch (e) {
                console.error(e);
                box.innerText = "추천 사유를 불러오지 못했습니다.";
            }
        };

        list.appendChild(card);
    });
}

/* ===============================
   포지션 유틸
================================ */
function displayPositionName(pos) {
    return {
        GK: "Goalkeeper",
        DF: "Defender",
        MF: "Midfielder",
        FW: "Attacker"
    }[pos] || pos;
}

//  비교용 표준화 함수 
function normalizePosition(pos) {
    return {
        GK: "Goalkeeper",
        DF: "Defender",
        MF: "Midfielder",
        FW: "Attacker"
    }[pos] || pos;
}
