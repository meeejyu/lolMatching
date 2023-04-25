# Matching System

### **✅**한줄 소개

게임 매칭 시스템의 구현


### 📖 상세 내용

즐겨하는 게임인 LOL의 매칭 시스템을 직접 만들어 보면 어떨까?해서 만들게된 프로젝트입니다. 팀의 인원, MMR의 격차, 게임 수락 시간 등 원하는 타입(Position, Rank, MMR, All)에 따른 매칭을 시킬수 있습니다. 누구나 쉽게 해당 코드를 풀 받아 커스텀하여 사용할 수 있습니다. 


### 📱 구현 기능 (API)

 타입에 따른 매칭 방법 TYPE = [’ALL’, MMR’, ‘POSITION’, ‘RANK’]
    
    ALL 타입 매칭 : mmr과 position, rank 3가지 요소 모두를 고려한 매칭
    
    MMR 타입 매칭 : mmr 요소만 고려한 매칭
    
    POSITION 타입 매칭 : mmr과 postion 요소만 고려한 매칭
    
    RANK 타입 매칭 : mmr과 rank 요소만 고려한 매칭

    공통 기능 : 

        1. 1차로 각 매칭인원의 2배를 모집한다. ex) 2:2 매칭일 경우 4명의 모집
        -- 모집 완료 전에는 중도 취소 가능, 이후에는 중도 취소 불가 -- 
        2. 모집이 완료 된 이후 한번 더 회원들에게 수락요청을 한다
        3. 수락이 모두 완료 되면 모집한 인원을 mmr을 고려하여 팀을 나눠준다
        4. 팀 매칭이 완료됨을 알려준다.
### 🛠️ 사용 기술 및 라이브러리

- java
- Spring Boot, Spring MVC
- Mybatis
- Gradle
- MariaDB, redis

### Project Sturucture
~~~
src/main/java/com/lol/match
|── common // 공통 컴포넌트(예외와 스케줄러 관리)
│   ├── exception
│   └── scheduler
├── config // redis 설정
├── main // 매칭 로직
│   ├── controller
│   ├── mapper
│   ├── model // 매칭 dto
│   └── service 
        ├── AllPositionService // all타입와 position 타입의 매칭
        └── MmrRankService // mmr타입와 rank 타입의 매칭
~~~

### ALL 타입 매칭 사용법
    
#### ERD
erd.png
<img width="1114" alt="erd" src="https://user-images.githubusercontent.com/112879800/234208908-c18fcbff-1bb2-4e34-9937-3d775ec6c513.png">


#### API
ALL 타입 매칭 API
- 매칭 (POST /match/all)
- 매칭 취소	(POST /match/all/cancel)
- 매칭 수락	(POST /match/all/accept)
- 매칭이 완료됐지만 아무도 수락하지 않은 경우 정보 삭제	(POST /match/delete/all/position/{listName})
- 매칭 완료된 팀 정보 (POST /match/all/complete)
- 게임 완료 후 팀 정보 삭제	(POST /match/end/{listName})

#### 구현 상세
>  매칭 로직은 다음과 같습니다.
>  1. 클라이언트로부터 userId를 보내 매칭 요청을 보낸다.
>  2. userId를 통해 회원 정보 및 setting 정보를 가져온다
>     
>      ** setting 정보란? : 매칭 인원, 수락시간, 매칭 범위 등을 가지고 있는 정보
>  3. 회원의 랭크와 포지션을 고려하여 일치하는 팀이 있는 경우 해당 팀에 정보를 추가한다.
>     일치하는 팀이 없는 경우 새로운 팀을 생성하여 회원 정보를 추가한다.
>  4. 팀을 배정받은 후 1시간 이내 팀 정원이 모두 차면 "success" 메세지를 생성한다.
>      1시간이 지나면 "auto_cancel" 메세지를, 회원이 매칭을 중도 취소한 경우는  "cancel" 메세지를 생성한다.
>  5. 생성된 message가 "scucess"일 경우 매칭 최대 수락시간을 저장 후 메세지를 내보낸다.
      아닌 경우 이전에 생성된 메세지를 그대로 내보낸다.
>
>  <img width="447" alt="스크린샷 2023-04-25 오후 5 15 51" src="https://user-images.githubusercontent.com/112879800/234216819-99ea8947-af96-40ba-958e-c57a2ecf1670.png">

<br>

> 매칭 수락 로직은 다음과 같습니다.
>  1. 클라이언트로부터 userId를 보내 매칭 요청을 보낸다.
>  2. userId를 통해 회원 정보 및 setting 정보, 매칭 최대 수락 시간을 가져온다
>  3. 매칭 최대 수락 시간이 회원이 수락한 시간보다 적고, 팀원 전체가 수락했을 경우 팀을 배분하고 "success" 메세지를 생성한다. 
> 4. 메세지가 success일 경우 바로 메세지를 내보내고
> 메세지가 없을 경우 저장된 팀 정보에서 수락하지 않은 회원을 지우고, fail 메세지를 생성하여 내보낸다.
> ![매칭수락](https://user-images.githubusercontent.com/112879800/234220149-a483086f-5ef7-4dea-951c-4759dab36da8.svg)

<br>

> 매칭 알고리즘은 다음과 같습니다.
> 1. 팀 정보를 전체 가져온다.
> 2. 가져온 정보를 토대로 임의로 A팀을 생성한다.
> 3. A팀에 배정된 회원을 제외한 나머진 회원을 B팀에 추가한다.
> 4. A팀과 B팀의 mmr 차이를 구하여 hash 키에 mmr차이를, value에 A팀과 B 팀 정보를 저장해준다.
> 5. 2~4번 과정을 반복하면서 hash key값이 0이거나, 새롭게 구한 A팀과 B팀의 mmr 차이가 key값보다 적으면 기존 해쉬 값을 지우고, hash 키를 새롭게 저장해준다. 
> 6. A팀과 B팀 배정의 모든 경우의 수를 구했을 경우 반복문을 탈출하고 hash에 저장되어 있는 팀정보로 매칭을 끝낸다.
> ![매칭알고리즘all](https://user-images.githubusercontent.com/112879800/234225355-4bc0ab58-bef7-42b9-b16e-edcc63268bc8.png)


### 그외 타입(MMR, POSITION, RANK) 매칭 방법 
- MMR 타입 : [MMR 타입 상세문서](https://wirehaired-waterfall-ea2.notion.site/Matcing-MMR-0ad018e093ec474c9b2d00948af29ed2)

- RANK 타입 : [RANK 타입 상세문서](https://wirehaired-waterfall-ea2.notion.site/Matcing-Rank-136a816ba60e4159900a1a399a37afe0)

- POSITION 타입 : [POSITION 타입 상세문서](https://wirehaired-waterfall-ea2.notion.site/Matcing-Position-df194d01522d42b3b52c713637428eba)

### **성과**

- 큐 시스템을 직접 만들어보며, 대기열에 대해 생각해보게 됨
- 조합 알고리즘을 응용하여 mmr 수치에 따른 공정한 매칭 구현
    - 조합이란 n개의 값 중에서 r개의 숫자를 순서를 고려하지 않고 나열한 경우의 수
    - 만약 3 : 3 매칭일 경우 모든 매칭의 경우의 수를 구하여 각 팀의 mmr 수치가 최소값일 때 최종 매칭
- 잦은 redis 통신 통해 부하가 발생할 수 있는 포인트 개선
    - 스레드의 sleep을 통해 CPU 독점 방지
- Global Exception Handler 구현을 통해 에러 관리