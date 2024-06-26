package study.springdatajpa.service;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.springdatajpa.entity.Member;
import study.springdatajpa.entity.Team;
import study.springdatajpa.entity.TeamMember;
import study.springdatajpa.repository.MemberRepository;
import study.springdatajpa.repository.TeamRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Slf4j
class TeamServiceTest {

    @Autowired
    private TeamService teamService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TeamRepository teamRepository;

    @Test
    @Transactional
//    @Commit
    void signUpMember() {

        // given...
        Member member = new Member("MEMBER1", 20);

        // when...
        Member member1 = memberService.signUpMember(member);

        // then...
        Assertions.assertThat(member1.getId()).isEqualTo(member.getId());
    }

    @Test
    @Transactional
    @Commit
    void createTeam() {
        // given...
        Team team = new Team("team 1");

        // when...
        Team team1 = teamService.createTeam(team);

        // then...
        Assertions.assertThat(team1.getId()).isEqualTo(team.getId());

    }

    @Test
    @Transactional
    @Rollback(false)
    void addMemberToTeam() {

        // given...
        Member member = new Member("MEMBER1", 20);
        memberService.signUpMember(member);
        Team team = new Team("team 1");
        teamService.createTeam(team);

        // when... V1
        // 객체 코드상에서 연관관계를 맺어야 cascade 옵션으로 저장이 되며, insert query update query 생성
        member.addTeam(team);
//        teamRepository.save(team);
//        memberRepository.save(member);

        // when... V2
//        teamRepository.save(team);
//        teamService.addMemberToTeam(member.getId(), team.getId());
//        memberService.addMemberToTeamV2(team, member);

        // then..
        Member member1 = memberService.findById(member.getId()).get();
        Team team1 = teamService.findById(team.getId()).get();

        assertThat(member1.getTeamMembers().size()).isEqualTo(1);
        TeamMember teamMember = member1.getTeamMembers().get(0);
        assertThat(teamMember.getTeam().getId()).isEqualTo(team1.getId());
        assertThat(teamMember.getMember().getId()).isEqualTo(member1.getId());

        // Log statements to verify the data
        log.info("member id : {}", member1.getId());
        log.info("team member count : {}", member1.getTeamMembers().size());
        if (!member1.getTeamMembers().isEmpty()) {
            log.info("team id : {}", member1.getTeamMembers().get(0).getTeam().getId());
            log.info("teamMember id : {}", member1.getTeamMembers().get(0).getId());
        } else {
            log.warn("Team members list is empty");
        }
    }
}