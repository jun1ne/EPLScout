async function runRecommendation() {
    const teamId = document.getElementById("teamId").value;
    const season = document.getElementById("season").value;

    //  추천 계산 실행
    await fetch(`/api/recommend/run?teamId=${teamId}&season=${season}`, {
        method: "POST"
    });

    //  결과 조회
    loadResults();
}

async function loadResults() {
    const teamId = document.getElementById("teamId").value;
    const season = document.getElementById("season").value;

    const res = await fetch(
        `/api/recommend/results?teamId=${teamId}&season=${season}`
    );
    const data = await res.json();

    const resultDiv = document.getElementById("result");
    resultDiv.innerHTML = "";

    data.forEach((p, idx) => {
        resultDiv.innerHTML += `
            <div class="card ${p.position}"
                 onclick="showReason(${p.player_id})">
                <b>${idx + 1}. ${p.name}</b> (${p.position})<br>
                점수: ${p.score.toFixed(2)} /
                포텐셜: ${p.potential.toFixed(2)}
            </div>
        `;
    });
}

/**
 *  선수 클릭 → 추천 사유 (LLM)
 */
async function showReason(playerId) {
    const teamId = document.getElementById("teamId").value;
    const season = document.getElementById("season").value;

    const res = await fetch(
        `/api/recommend/reason?teamId=${teamId}&playerId=${playerId}&season=${season}`
    );
    const text = await res.text();

    alert(text);
}
