document.addEventListener("DOMContentLoaded", loadTeams);

async function loadTeams() {

    const season = 2025;   // 고정 테스트용
    const grid = document.getElementById("teamGrid");

    try {

        const res = await fetch(`/api/team/list?season=${season}`);
        const teams = await res.json();

        grid.innerHTML = "";

        teams.forEach(team => {

            const card = document.createElement("div");
            card.className = "team-card";

            card.innerHTML = `
                <div class="team-card-inner">
                    <img src="${team.logoUrl ?? '/images/default.png'}"
                         class="team-logo-large">
                    <h3>${team.name}</h3>
                </div>
            `;

            card.onclick = () => {
                location.href =
                    `/team.html?teamId=${team.teamId}&season=${season}`;
            };

            grid.appendChild(card);
        });

    } catch (e) {
        grid.innerHTML = "팀 로드 실패";
        console.error(e);
    }
}
