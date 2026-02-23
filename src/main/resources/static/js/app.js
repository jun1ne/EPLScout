console.log("APP JS LOADED");

document.addEventListener("DOMContentLoaded", async () => {

    try {
        await loadTeams();
    } catch (e) {
        console.error("초기 팀 로드 실패", e);
    }

    autoRunFromQuery();

    const recommendBtn = document.getElementById("recommendBtn");
    if (recommendBtn) {
        recommendBtn.addEventListener("click", recommendPlayers);
    }

    //  시즌 변경 시 팀 목록 다시 로드
    const seasonInput = document.getElementById("seasonInput");
    if (seasonInput) {
        seasonInput.addEventListener("change", async () => {
            await loadTeams();
            // 시즌 바뀌면 이전 팀 선택 초기화
            const teamSelect = document.getElementById("teamSelect");
            if (teamSelect) teamSelect.value = "";
        });
    }
});

/* ===============================
   URL 파라미터 자동 실행
================================ */
function autoRunFromQuery() {

    const params = new URLSearchParams(location.search);
    const teamId = params.get("teamId");
    const season = params.get("season");

    if (!teamId) return;

    const seasonInput = document.getElementById("seasonInput");
    if (season && seasonInput) {
        seasonInput.value = season;
    }

    setTimeout(() => {
        const teamSelect = document.getElementById("teamSelect");
        if (teamSelect) {
            teamSelect.value = teamId;
            recommendPlayers();
        }
    }, 300);
}

/* ===============================
   팀 목록 로드
================================ */
async function loadTeams() {

    const seasonInput = document.getElementById("seasonInput");
    const select = document.getElementById("teamSelect");

    if (!seasonInput || !select) {
        console.error("teamSelect 또는 seasonInput 없음");
        return;
    }

    const season = seasonInput.value;

    try {

        const res = await fetch(`/api/team/list?season=${season}`);

        if (!res.ok) {
            throw new Error("팀 API 응답 실패");
        }

        const teams = await res.json();

        select.innerHTML = `<option value="">팀 선택</option>`;

        if (!teams || teams.length === 0) {
            console.warn("해당 시즌 팀 데이터 없음");
            return;
        }

        teams.forEach(team => {
            const opt = document.createElement("option");
            opt.value = team.teamId;
            opt.textContent = team.name;
            select.appendChild(opt);
        });

    } catch (e) {
        console.error("팀 로드 에러:", e);
        select.innerHTML = `<option value="">팀 로드 실패</option>`;
    }
}

/* ===============================
   추천 실행
================================ */
async function recommendPlayers() {

    const teamSelect = document.getElementById("teamSelect");
    const seasonInput = document.getElementById("seasonInput");

    if (!teamSelect || !seasonInput) return;

    const teamId = teamSelect.value;
    const season = seasonInput.value;

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
        await renderRecommendList(data.recommendations, summary);
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

    const logoEl = document.getElementById("teamLogoImg");
    if (logoEl) {
        if (data.teamLogo) {
            logoEl.src = data.teamLogo;
            logoEl.style.display = "block";
        } else {
            logoEl.style.display = "none";
        }
    }

    const nameEl = document.getElementById("teamNameTitle");
    if (nameEl && data.teamName) {
        nameEl.innerText = data.teamName + " 분석 결과";
    }

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
                평점 기준 취약 포지션:
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
async function renderRecommendList(players, summary) {

    const list = document.getElementById("recommendList");
    list.innerHTML = "";

    if (!players || players.length === 0) {
        list.innerHTML = "<p>추천 영입 후보가 없습니다.</p>";
        return;
    }

    const season = document.getElementById("seasonInput").value;
    const weakPositions = summary?.weakPositions || [];

    for (const player of players) {

        const scoutScore = Number(player.score ?? 0);

        let marketValue = 0;
        let grade = "C";

        try {
            const valueRes =
                await fetch(`/api/player/${player.playerId}/value?season=${season}`);
            const valueData = await valueRes.json();

            marketValue = Number(valueData.valueScore ?? 0);
            grade = valueData.grade ?? "C";

        } catch (e) {
            console.error("Market Value 로드 실패", e);
        }

        const potential = Number(player.potentialScore ?? 0);

        const isWeak =
            weakPositions.includes(
                normalizePosition(player.position)
            );

        const scoutPercent = Math.min(scoutScore, 100);
        const valuePercent = Math.min(marketValue, 100);
        const displayGrade = grade === "Splus" ? "S+" : grade;

        const card = document.createElement("div");
        card.className = "recommend-card";

        if (grade === "Splus") {
            card.classList.add("card-splus");
        }

        card.style.cursor = "pointer";
        card.addEventListener("click", (e) => {
            if (e.target.closest(".toggle-btn")) return;
            window.location.href =
                `/player.html?playerId=${player.playerId}&season=${season}`;
        });

        card.innerHTML = `
            <div class="player-header">
                <div class="player-info">
                    <img src="${player.photoUrl ?? ''}"
                         class="player-avatar"
                         onerror="this.style.display='none'">
                    <div>
                        <strong>${player.playerName}</strong> (${player.age}세)<br>
                        ${displayPositionName(player.position)}
                        · 스카우트 점수 ${scoutScore.toFixed(1)}점
                        · 성장 잠재력 ${potential.toFixed(2)}
                        ${isWeak ? `<span class="badge warning">팀 취약 포지션</span>` : ""}
                    </div>
                </div>
                <div class="right-section">
                    <div class="value-badge grade-${grade}">
                        선수 등급 ${displayGrade} (${marketValue.toFixed(1)}점)
                    </div>
                    <button class="btn small toggle-btn">추천 이유 보기</button>
                </div>
            </div>
            <div class="score-bar">
                <div class="score-fill" style="width:${scoutPercent}%"></div>
            </div>
            <div class="value-bar">
                <div class="value-fill grade-${grade}" style="width:${valuePercent}%"></div>
            </div>
            <div class="reason-box" style="display:none;"></div>
        `;

        const btn = card.querySelector(".toggle-btn");
        const box = card.querySelector(".reason-box");
        let loaded = false;

        btn.onclick = async () => {

            if (box.style.display === "block") {
                box.style.display = "none";
                btn.innerText = "추천 이유 보기";
                return;
            }

            box.style.display = "block";
            btn.innerText = "닫기";

            if (loaded) return;

            box.innerText = "추천 이유 생성 중...";

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
                            score: scoutScore,
                            potentialScore: potential
                        })
                    }
                );

                box.innerText = await res.text();
                loaded = true;

            } catch (e) {
                console.error(e);
                box.innerText = "추천 이유를 불러오지 못했습니다.";
            }
        };

        list.appendChild(card);
    }
}

/* ===============================
   가치 등급 계산
================================ */
function calculateGrade(value) {
    if (value >= 95) return "Splus";
    if (value >= 90) return "S";
    if (value >= 80) return "A";
    if (value >= 70) return "B";
    return "C";
}

/* ===============================
   포지션 유틸
================================ */
function displayPositionName(pos) {
    return {
        GK: "골키퍼",
        DF: "수비수",
        MF: "미드필더",
        FW: "공격수"
    }[pos] || pos;
}

function normalizePosition(pos) {
    return {
        GK: "Goalkeeper",
        DF: "Defender",
        MF: "Midfielder",
        FW: "Attacker"
    }[pos] || pos;
}
