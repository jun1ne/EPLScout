import service.TeamApiService;
import model.Team;
import java.util.List;

public class App {

    public static void main(String[] args) {

        TeamApiService service = new TeamApiService();

        try {
            List<Team> teams = service.getEplTeams();

            for (Team team : teams) {
                System.out.println(
                    team.getApiTeamId() + " | " + team.getName()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
