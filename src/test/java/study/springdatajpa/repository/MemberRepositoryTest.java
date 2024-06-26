package study.springdatajpa.repository;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.springdatajpa.entity.Member;
import study.springdatajpa.entity.Team;
import study.springdatajpa.repository.query.MemberQueryDto;
import study.springdatajpa.service.MemberService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@SpringBootTest
@Slf4j
class MemberRepositoryTest {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private EntityManager em;

    @Test
    @Transactional
    @Commit
        // test code에서는 insert query 로그 안남음 -> rollback false
    void memberSaveTest() {

        // given...
        Member member = new Member("USER1", 00);

        // when...
        memberService.signUpMember(member); // 1차 캐싱 된 entity 자동 반환.
        log.info("saved member id : {}", member.getId());
        log.info("member value : {}", member);
        Member byId = memberService.findById(member.getId()).get();

        // jpa의 동일 트랜잭션에서는 5가지 특징
        // 1. 동일성 보장 = 1차 캐싱
        // 2. 변경 감지
        // 3. lazy loading
        // 4. 쓰기 지연 저장소

        // then...
        org.assertj.core.api.Assertions.assertThat(byId.getId()).isEqualTo(member.getId());

    }


    @Test
    void memberFindByIdTest() {
        log.info("memberRepository Value : {}", memberRepository.getClass());
        log.info("member count : {}", memberRepository.count());
    }

    @Test
    void findMemberTest() {

        Member member = new Member("USER1", 00);
        Member member1 = new Member("USER1", 01);
        Member member2 = new Member("USER123", 02);
        List<Member> list = Arrays.asList(member1, member, member2);

        memberRepository.saveAll(list);
        memberRepository.findByMemberNameAfter(member.getMemberName()).forEach(member3 -> {
            log.info("member name : {}", member3.getMemberName());
            log.info("member age : {}", member3.getAge());
        });
        memberRepository.findByMemberNameContaining(member.getMemberName());
//                .forEach(member3 -> {
//            log.info("member name : {}", member3.getMemberName());
//            log.info("member age : {}", member3.getAge());
//        });
//        memberRepository.findByMemberNameAndAgeGreaterThan(member.getMemberName(), member.getAge()).forEach(member3 -> {
//            log.info("member name : {}", member3.getMemberName());
//            log.info("member age : {}", member3.getAge());
//        });

    }


    @Test
    void findMemberBy() {

        Member member = new Member("USER1", 00);
        Member member1 = new Member("USER1", 01);
        Member member2 = new Member("USER123", 02);
        List<Member> list = Arrays.asList(member1, member, member2);
        memberRepository.saveAll(list);

        memberRepository.findMemberBy();
        memberRepository.findAll();
        memberRepository.findTop100MemberBy();
        memberRepository.findByMemberName("USER1");
        List<String> allByMemberName = memberRepository.findAllByMemberName();

        Assertions.assertThat(allByMemberName).hasSize(3);
    }


    @Test
    @Transactional
    @Commit
    void findAllByMemberDto() {

        // given...
        Member member = new Member("MEMBER1", 20);
        Member member1 = new Member("MEMBER2", 21);
        Member member2 = new Member("MEMBER3", 21);
        Member member3 = new Member("MEMBER4", 21);
        List<Member> list = Arrays.asList(member1, member, member2, member3);
        memberRepository.saveAll(list);

        Team team = new Team("team 1");
        Team team1 = new Team("team 2");
        Team team2 = new Team("team 3");
        List<Team> list1 = Arrays.asList(team1, team2, team);
        teamRepository.saveAll(list1);

        member.addTeam(team);
        member1.addTeam(team);
        member2.addTeam(team);
        member3.addTeam(team1);


        List<MemberQueryDto> membersByTeamId = memberRepository.findMembersByTeamId(team.getId());

        Assertions.assertThat(membersByTeamId).hasSize(3);

    }

    @Test
    void returnParameterTest() {

        Member member = new Member("MEMBER1", 20);
        Member member1 = new Member("MEMBER2", 21);
        Member member2 = new Member("MEMBER3", 21);
        Member member3 = new Member("MEMBER4", 21);
        List<Member> list1 = Arrays.asList(member, member1, member2, member3);
        memberRepository.saveAll(list1);

        Member byMemberName = memberRepository.findByMemberName(member.getMemberName());
        List<Member> allByAge = memberRepository.findAllByAge(21);
        Optional<List<Member>> byAge = memberRepository.findByAge(21);

        Assertions.assertThat(byMemberName.getMemberName()).isEqualTo(member.getMemberName());
        Assertions.assertThat(allByAge).hasSize(3);
        Assertions.assertThat(byAge.get().get(0).getAge()).isEqualTo(21);

    }


    /**
     * Paging and Sorting Test
     */
    @Test
    @Transactional
    @Commit
    void findByAgeWithPaging() {

        List<Member> collect = IntStream.rangeClosed(1, 31).mapToObj(i -> {
            Member member = new Member("MEMBER" + i, i);
            memberRepository.save(member);
            Team team = new Team("team" + i);
            teamRepository.save(team);
            member.addTeam(team);
            return member;
        }).collect(Collectors.toList());

        int currentPage = 3;
        int offset = 0;
        int limit = 10;

        Page<Member> byAgeWithPaging = memberService.findAllByPaging(currentPage, limit);
        log.info("========= total ==========");
        log.info("total content count : {}", byAgeWithPaging.getTotalElements());
        log.info("total page : {}", byAgeWithPaging.getTotalPages());

        log.info("========= Current request ==========");
        log.info("current request total size (limit) : {}", byAgeWithPaging.getSize());
        log.info("current page Number : {}", byAgeWithPaging.getNumber());
        log.info("first page ?: {}", byAgeWithPaging.isFirst());
        log.info("next page ? : {}", byAgeWithPaging.hasNext());

        /**
         *  === page　메서드 검증
         * @int requestCurrentPage = 0
         * @limit 10
         * ==== Total ====
         * 1. Total content count : getTotalElements();
         * 2. Total page : getTotalPages();
         * ==== Current Request ====
         * 3. total page count (limit) : getSize();
         * 4. Current Page Number : getNumber();
         */
        Assertions.assertThat(byAgeWithPaging.getTotalElements()).isEqualTo(31);
        Assertions.assertThat(byAgeWithPaging.getTotalPages()).isEqualTo(4);
        Assertions.assertThat(byAgeWithPaging.getSize()).isEqualTo(10);
        Assertions.assertThat(byAgeWithPaging.getNumber()).isEqualTo(3);
        Assertions.assertThat(byAgeWithPaging.isFirst()).isEqualTo(false);
        Assertions.assertThat(byAgeWithPaging.isLast()).isEqualTo(true);
        Assertions.assertThat(byAgeWithPaging.hasNext()).isEqualTo(false);

//        byAgeWithPaging.getContent().stream().forEach(content -> {
//            log.info("page : {}, content number : {}", byAgeWithPaging.getNumber(), content.getAge());
//        });

    }

    /**
     * Paging and Sorting and toMap Test
     */
    @Test
    @Transactional
    @Commit
    void findMemberWithPagingToDto() {


        IntStream.rangeClosed(1, 31).mapToObj(i -> {
            Member member = new Member("MEMBER" + i, i);
            memberRepository.save(member);
            Team team = new Team("team" + i);
            teamRepository.save(team);
            member.addTeam(team);
            return member;
        }).collect(Collectors.toList());

        int currentPage = 0;
        int offset = 0;
        int limit = 10;

        Page<Member> byAgeWithPaging = memberService.findAllByPaging(currentPage, limit);
        Page<MemberQueryDto> map = byAgeWithPaging.map(member -> new MemberQueryDto(member.getId(), member.getMemberName(), member.getAge()));
        log.info("========= total ==========");
        log.info("total content count : {}", map.getTotalElements());
        log.info("total page : {}", map.getTotalPages());

        log.info("========= Current request ==========");
        log.info("current request total size (limit) : {}", map.getSize());
        log.info("current page Number : {}", map.getNumber());
        log.info("first page ?: {}", map.isFirst());
        log.info("next page ? : {}", map.hasNext());

        /**
         *  === page　메서드 검증
         * @int requestCurrentPage = 0
         * @limit 10
         * ==== Total ====
         * 1. Total content count : getTotalElements();
         * 2. Total page : getTotalPages();
         * ==== Current Request ====
         * 3. total page count (limit) : getSize();
         * 4. Current Page Number : getNumber();
         */
        /**
         *  === page　메서드 검증
         * @int requestCurrentPage = 0
         * @limit 10
         * ==== Total ====
         * 1. Total content count : getTotalElements();
         * 2. Total page : getTotalPages();
         * ==== Current Request ====
         * 3. total page count (limit) : getSize();
         * 4. Current Page Number : getNumber();
         */
        Assertions.assertThat(byAgeWithPaging.getTotalElements()).isEqualTo(31);
        Assertions.assertThat(byAgeWithPaging.getTotalPages()).isEqualTo(4);
        Assertions.assertThat(byAgeWithPaging.getSize()).isEqualTo(10);
        Assertions.assertThat(byAgeWithPaging.getNumber()).isEqualTo(3);
        Assertions.assertThat(byAgeWithPaging.isFirst()).isEqualTo(false);
        Assertions.assertThat(byAgeWithPaging.isLast()).isEqualTo(true);
        Assertions.assertThat(byAgeWithPaging.hasNext()).isEqualTo(false);

        map.getContent().stream().forEach(content -> {
            log.info("page : {}, content number : {}", byAgeWithPaging.getNumber(), content.getAge());
        });


    }


    @Test
    @Transactional
    @Commit
    public void bulkUpdateAgePlus() {
        List<Member> collect = IntStream.rangeClosed(0, 30).mapToObj(i -> {
            Member member = new Member("user" + i, 10);
            return member;
        }).collect(Collectors.toList());
        memberRepository.saveAll(collect);

        memberService.memberBulkUpdate(10);

        memberRepository.findAll().forEach(member ->{
            log.info("member name : {}", member.getMemberName());
            log.info("age : {}", member.getAge());
            Assertions.assertThat(member.getAge()).isEqualTo(11);
        });

    }


    @Test
    void findMemberLazy() {

        createMember();

        memberRepository.findAll().forEach(member -> {
            log.info("memberName : {}", member.getMemberName());
            member.getTeamMembers().forEach(teamMember -> {
                log.info("member's Team class : {}", teamMember.getTeam().getClass());
                log.info("member's Team name : {}", teamMember.getTeam().getName());
            });
        });
    }

    @Test
    @Transactional
    void findMemberFetchJoin() {

        createMember();

        memberRepository.findMemberFetchJoin().forEach(member -> {
            log.info("memberName : {}", member.getMemberName());
            member.getTeamMembers().forEach(teamMember -> {
                log.info("member's Team class : {}", teamMember.getTeam().getClass());
                log.info("member's Team name : {}", teamMember.getTeam().getName());
            });
        });
    }

    @Test
    @Transactional
    void findMemberEntityGraph() {

        createMember();

        //findAllEntityGraph()
        memberRepository.findAll().forEach(member -> {
            log.info("memberName : {}", member.getMemberName());
            member.getTeamMembers().forEach(teamMember -> {
                log.info("member's Team class : {}", teamMember.getTeam().getClass());
                log.info("member's Team name : {}", teamMember.getTeam().getName());
            });
        });

        Member member1 = memberRepository.findByMemberName("Member1");
        log.info("member's Team name : {}", member1.getMemberName());
        member1.getTeamMembers().forEach(teamMember -> {
            log.info("member's Team class : {}", teamMember.getTeam().getClass());
            log.info("member's Team name : {}", teamMember.getTeam().getName());
        });

    }


    private void createMember() {
        IntStream.rangeClosed(1, 10).mapToObj(i -> {
            Member member = new Member("Member" + i, i);
            memberRepository.save(member);
            Team team = new Team("Team" + i);
            teamRepository.save(team);
            member.addTeam(team);
            return member;
        }).collect(Collectors.toList());
    }


    @Test
    @Transactional
    void findMemberQueryHintReadOnly() {

        // given...
        createMember();
        em.flush();
        em.clear();

        // when...
        String memberName = "Member1";
        String updatedMemberName = "Member2";
        Member readOnlyByMemberName = memberService.findReadOnlyByMemberName(memberName);
        readOnlyByMemberName.setMemberName("dddd");


        // then..
        Assertions.assertThat(readOnlyByMemberName.getMemberName()).isEqualTo(updatedMemberName);

    }

    @Test
    @Transactional
    @Commit
    void findMemberMergeAbdDirtyChecking() {

        // given...
        String memberName = "Member1";
        String updatedMemberName = "Member2";

        Member member = new Member(memberName, 20);
        member.setId(0L);
        Member save = memberRepository.save(member);

        // when...
        Optional<Member> byId = memberService.findById(save.getId());
        // 영속성 컨텍스트로 save()된 엔티티는 별도 엔티티를 반환받지 않더라도 자동으로 해당 pk 값을 설정해서 반환한다.
        // 다만, 기존 id 값이 설정된 준영속화된 엔티티들은 변경사항에 대하여 1차 캐싱된 이후 해당 값을 반환 받아야 한다.
        Member persistedMember = byId.get();
        log.info("member : {}", persistedMember.getMemberName());
        log.info("member id : {}", persistedMember.getId());

        // 변경감지 테스트
        persistedMember.setMemberName(updatedMemberName);

        // then..
        log.info("updated member name : {}", persistedMember.getMemberName());
        Assertions.assertThat(persistedMember.getMemberName()).isEqualTo(updatedMemberName);

    }


    @Test
    @Transactional
    public void callCustom() {

        // given...
        createMember();
        String memberName = "Member1";
        List<Member> membersByMemberName = memberRepository.findAllByMemberName(memberName);
    }


    @Test
    void projectionsInterface() {

        // given...
        createMember();

        // when...
        List<MemberNameOnlyDtoInterface> memberNames = memberRepository.findProjectionsByMemberNameContaining("Member");
        log.info("size : {} ", memberNames.size());
        memberNames.stream().forEach(member -> {
            log.info("member name : {}", member.getMemberName());
//            log.info("member name+age : {}", member.getUserNameAndAge());

        });
    }

    @Test
    void projectionsClass() {
        // given...
        createMember();

        // when...
        List<MemberNameOnlyDtoClass> memberNames = memberRepository.findProjectionByMemberNameContaining("Member");
        log.info("size : {} ", memberNames.size());
        memberNames.stream().forEach(member -> {
            log.info("member name : {}", member.getMemberName());
            log.info("member age : {}", member.getAge());
//            log.info("member name+age : {}", member.getUserNameAndAge());
        });
    }

    @Test
    void dynamicProjectionsClass() {
        // given...
        createMember();

        // when...
        List<MemberNameOnlyDtoClass> memberNames = memberRepository.findDynamicProjectionByMemberNameContaining("member", MemberNameOnlyDtoClass.class);
        log.info("size : {} ", memberNames.size());
        memberNames.stream().forEach(member -> {
            log.info("member name : {}", member.getMemberName());
            log.info("member age : {}", member.getAge());
        });
    }

    @Test
    void nestedProjectionsClass() { //중첩 프로젝션
        // given...
        createMember();

        // when...
        List<NestedClosedProjections> memberNames = memberRepository.findNestedProjectionByMemberNameContaining("member", NestedClosedProjections.class);
        log.info("size : {} ", memberNames.size());
        memberNames.stream().forEach(member -> {
            log.info("member name : {}", member.getMemberName());
            member.getTeamMembers().forEach(teamMember -> {
                log.info("member Team name : {}", teamMember.getTeam().getName());
            });
        });
    }

    @Test
    void nativeQueryTest() { //중첩 프로젝션
        // given...
        createMember();

        // when...
        List<Member> member1 = memberRepository.findMemberByNativeQuery("Member1");
        log.info("size : {} ", member1.size());
        member1.stream().forEach(member -> {
            log.info("member name : {}", member.getMemberName());
        });
    }

    @Test
    @Transactional
    @Commit
    void nativeQueryPagingTest() { //중첩 프로젝션
        // given...
        createMember();

        // when...
        PageRequest pageRequest = PageRequest.of(0, 5);
        Page<MemberProjection> byPagingNativeProjection = memberRepository.findByPagingNativeProjection(pageRequest);
        log.info("size : {} ", byPagingNativeProjection.getSize());
        byPagingNativeProjection.stream().forEach(member -> {
            log.info("member name : {}", member.getId());
            log.info("member name : {}", member.getMemberName());
            log.info("member Team name : {}", member.getTeamName());
        });
    }

}


