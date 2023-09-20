package com.kb04.starroad.Controller;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.board.BoardRequestDto;
import com.kb04.starroad.Dto.board.BoardResponseDto;
import com.kb04.starroad.Dto.board.CommentDto;
import com.kb04.starroad.Entity.Board;
import com.kb04.starroad.Service.BoardService;
import com.kb04.starroad.Service.CommentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Api(tags = "게시판 API")
@RestController
public class BoardController {


    private final BoardService boardService;

    private final CommentService commentService;

    public BoardController(BoardService boardService, CommentService commentService) {
        this.boardService = boardService;
        this.commentService = commentService;
    }

    //자유랑 인증 게시판
    @ApiOperation(value = "게시판 메인", notes = "게시판 메인화면을 보여줍니다")
    @GetMapping("/starroad/board/main")
    public ModelAndView boardList() {
        ModelAndView mav = new ModelAndView("board/main");

        // PageRequest를 생성하여 정렬 조건을 설정
        PageRequest freepageRequest = PageRequest.of(0, 10, Sort.by("regdate").descending());
        PageRequest authpageRequest = PageRequest.of(0, 10, Sort.by("regdate").descending());
        PageRequest popularpageRequest = PageRequest.of(0, 10, Sort.by("regdate").descending());

        mav.addObject("freeBoard", boardService.boardListFree(freepageRequest));
        mav.addObject("authBoard", boardService.boardListAuth(authpageRequest));
        mav.addObject("popularBoard", boardService.boardListpopular(popularpageRequest));

        return mav;
    }

    @ApiOperation(value = "게시물 글쓰기 폼", notes = "게시물 글쓰기 폼으로 이동합니다")
    @GetMapping("/starroad/board/write")
    public ModelAndView board(
            @ApiIgnore HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            ModelAndView mav = new ModelAndView("redirect:/starroad/login");
            mav.addObject("message", "로그인 후에 댓글을 작성할 수 있습니다.");
            return mav;
        }
        ModelAndView mav = new ModelAndView("board/write");
        return mav;

    }

    //자유게시판,인증게시판 요청
    @ApiOperation(value = "자유,인증 게시판", notes = "자유,인증 게시판으로 갈 수 있습니다.")
    @GetMapping("/starroad/board/free")
    public ModelAndView boardList(
            @ApiParam(value = "페이지번호") @RequestParam(name = "page", defaultValue = "0") int page,
            @ApiParam(value = "페이지크기") @RequestParam(name = "size", defaultValue = "6") int size,
            @ApiParam(value = "게시글타입") @RequestParam(name = "type", defaultValue = "F") String type,
            @ApiIgnore HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("board/board_new");


        // 페이징 정보 설정
        PageRequest pageable = PageRequest.of(page, size, Sort.by("regdate").descending());
        Page<Board> boardPage;
        Page<BoardResponseDto> finalBoardPage = null;

        if ("F".equals(type)) {
            // type이 "F"인 경우 자유게시판 목록 조회
            boardPage = boardService.findPaginated(pageable);
            finalBoardPage = boardService.convertPaginated(boardPage);

        } else if ("C".equals(type)) {
            // type이 "C"인 경우 인증방 목록 조회
            boardPage = boardService.findAuthenticatedPaginated(pageable);
            finalBoardPage = boardService.convertPaginated(boardPage);
        }
        else {
            // 잘못된 type 값이 들어온 경우 예외 처리
            throw new IllegalArgumentException("잘못된 type 값입니다.");
        }


        mav.addObject("freeBoardPage", finalBoardPage);
        mav.addObject("type", type); // View에서 현재 type 값을 사용할 수 있도록 추가

        return mav;
    }

    //인기게시판 요청
    @ApiOperation(value = "인기게시판", notes = "인기게시판을 보여줍니다")
    @GetMapping("/starroad/board/popular")
    public ModelAndView popularBoardList(
            @ApiParam(value = "페이지") @RequestParam(name = "page", defaultValue = "0") int page,
            @ApiParam(value = "페이지크기") @RequestParam(name = "size", defaultValue = "6") int size,
            @ApiIgnore HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("board/board_new");

        // 페이징 정보 설정
        PageRequest pageable = PageRequest.of(page, size, Sort.by("likes").descending());

        // 게시글 목록 조회 (likes 내림차순)
        Page<Board> boardPage = boardService.getPopularBoards(pageable);
        Page<BoardResponseDto> finalBoardPage = boardService.convertPaginated(boardPage);
        //웹 페이지로 데이터를 전달하기 위한 객체로, 이를 통해 뷰(HTML 템플릿)로 데이터를 전송
        mav.addObject("popularBoardPage", finalBoardPage);
        //ModelAndView에 "type"이라는 키를 사용하여 "popular" 문자열을 추가
        mav.addObject("type", "popular"); // 인기 게시판임을 표시하기 위한 값

        return mav;
        //인기 게시글을 조회하여 해당 게시글 목록을 boardPage에 저장하고,
        // 이를 ModelAndView에 추가하여 뷰로 전달하는 역할
    }

    @ApiOperation(value = "게시글 수정 폼", notes = "게시글을 폼을 볼 수 있습니다")
    @GetMapping("/starroad/board/update")
    public ModelAndView updateBoard(
            @ApiParam(value = "게시글 번호") @RequestParam("no") Integer no,
            @ApiIgnore HttpSession session) {


        ModelAndView mav = new ModelAndView("board/update");

        // 세션에서 현재 로그인한 사용자의 ID 가져오기
        MemberDto currentUser = (MemberDto) session.getAttribute("currentUser");
        String currentUserId = currentUser.getId();  // 사용자 ID 가져오기


        if (boardService.canUpdate(no, currentUserId)) {
            // 현재 사용자가 게시글 수정 가능한 경우
            Optional<Board> boardOptional = boardService.findById(no);


            Board board = boardOptional.get();
            BoardResponseDto boardResponseDto = board.toBoardResponseDto();

            mav.addObject("board", boardResponseDto);
        }
        else{
            // 현재 사용자가 게시글 수정 불가능한 경우 or 게시글 존재하지 않는 경우
            mav.setViewName("redirect:/starroad/board/detail?no=" + no);
        }
        return mav;

    }


    @ApiOperation(value = "게시글 수정기능", notes = "게시글을 수정할 수 있습니다")
    @PostMapping("/starroad/board/updatepro")
    public ModelAndView updateBoardPro(
            @ApiParam(value = "게시글 수정하기 위한 정보") @RequestBody @ModelAttribute BoardRequestDto boardRequestDto,
            @ApiParam(value = "이미지") @RequestParam(value = "image", required = false) MultipartFile image) {

        if (image != null && !image.isEmpty()) {
            try {
                // 이미지가 업로드된 경우에만 이미지 데이터를 설정합니다.
                byte[] imageBytes = image.getBytes();
                boardRequestDto.setImage(imageBytes);
            } catch (IOException e) {
                // 이미지 처리 중 예외 발생 시 처리 로직을 추가합니다.
                e.printStackTrace();
                // 예외 처리 방법에 따라 적절한 응답을 반환할 수 있습니다.
                ModelAndView errorModelAndView = new ModelAndView("error"); // 에러 페이지로 이동하는 예시
                return errorModelAndView;
            }
        }

        // 게시물 수정을 위해 서비스 메서드를 호출합니다.
        // BoardService의 메서드에 해당하는 로직을 호출해야 합니다.
        boardService.updateBoard(boardRequestDto); // 이 부분은 실제 서비스 메서드로 대체되어야 합니다.

        // 수정이 완료되면 원하는 페이지로 리다이렉트할 수 있습니다.

        ModelAndView modelAndView = new ModelAndView("redirect:/starroad/board/detail?no=" + boardRequestDto.getNo()); // 수정된 게시물로 이동하는 예시
        return modelAndView;

    }




    @ApiOperation(value = "게시글 작성", notes = "게시글을 작성할 수 있습니다")
    @PostMapping("/starroad/board/writepro")
    public ModelAndView boardWritePro(
            @ApiIgnore HttpSession session,
            @ApiParam(value = "게시글 종류") @RequestParam("type") String type,
            @ApiParam(value = "게시글 상세 종류") @RequestParam("detailType") String detailType,
            @ApiParam(value = "게시글 제목") @RequestParam("title") String title,
            @ApiParam(value = "게시글 내용") @RequestParam("content") String content,
            @ApiParam(value = "게시글 이미지") @RequestParam("image") MultipartFile imageFile
    ) {
//        // 세션에서 현재 로그인한 사용자의 정보 가져오기
//        Member currentUser = (Member) session.getAttribute("currentUser");

        if (session.getAttribute("currentUser") == null) {
            // 로그인하지 않은 사용자가 글 작성을 시도하는 경우 처리
            ModelAndView mav = new ModelAndView("redirect:/starroad/login");
            mav.addObject("message", "로그인 후에 게시글을 작성할 수 있습니다.");

            return mav;
        }

        try {
            MemberDto dto = (MemberDto) session.getAttribute("currentUser");
            // 게시글 작성 서비스 호출 시, 현재 사용자 ID 추가로 전달
            boardService.writeBoard(dto.getId(), type, detailType, title, content, imageFile);

            return new ModelAndView("redirect:/starroad/board/main");
        } catch (IOException e) {
            e.printStackTrace();

            // 이미지 업로드 실패 시 에러 페이지로 이동
            ModelAndView mav = new ModelAndView("/error");
            mav.addObject("message", "이미지 업로드에 실패했습니다.");

            return mav;
        }
    }

    @ApiOperation(value = "게시글 삭제", notes = "게시글을 삭제할 수 있습니다")
    @GetMapping("/starroad/board/delete")
    public ModelAndView deleteBoard(
            @ApiParam(value = "게시글 번호") @RequestParam Integer no,
            @ApiIgnore HttpSession session) {
        ModelAndView mav = new ModelAndView();

        if (session.getAttribute("currentUser") == null) {
            mav = new ModelAndView("redirect:/starroad/login");
            mav.addObject("message", "로그인 후에 댓글을 작성할 수 있습니다.");
            return mav;
        }

        MemberDto currentUser = (MemberDto) session.getAttribute("currentUser");
        String currentUserId = currentUser.getId();


        if (boardService.canDelete(no, currentUserId)) {
            // 현재 사용자가 게시글 삭제 가능한 경우
            boardService.deleteBoard(no);
            mav.setViewName("redirect:/starroad/board/main");

        } else{
            // 현재 사용자가 게시글 삭제 불가
            // 능한 경우 or 게시글 존재하지 않는 경우
            mav.setViewName("board/deleteError");
        }

        return mav;
    }


    @ApiOperation(value = "게시글 상세", notes = "게시글을 상세를 볼 수 있습니다")
    @GetMapping("/starroad/board/detail")
    public ModelAndView getBoardDetail(
            @ApiParam(value = "게시글 번호") @RequestParam("no") Integer no) {

        ModelAndView mav = new ModelAndView("board/detail");

        Optional<Board> boardOptional = boardService.findById(no);

        if (boardOptional.isPresent()) {
            Board board = boardOptional.get();

            List<CommentDto> comments = commentService.findByBoard(board);

            BoardResponseDto boardResponseDto = board.toBoardResponseDto();
            String memberId = board.getMember().getId();  // 'getId()'는 실제 회원 ID를 반환하는 메서드로 변경해야 합니다.
            boardResponseDto.setMemberId(memberId);  // 'setMemberId()'는 DTO에서 회원 ID를 설정하는 메서드입니다.
            //boardResponseDto.setCommentNum(board.getCommentNum()); // commentNum 수정
            boardResponseDto.setComments(comments);

            // 이미지를 Base64로 인코딩하여 DTO에 추가
            if (board.getImage() != null) {
                byte[] imageBytes = board.getImage();
                String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
                boardResponseDto.setImageBase64(imageBase64);
            }

            if (boardResponseDto.getComments() == null || boardResponseDto.getComments().isEmpty()) {
                mav.addObject("noComments", true);
            }

            mav.addObject("board", boardResponseDto);
        } else {
            mav.addObject("error", "게시글을 찾을 수 없습니다.");
        }

        return mav;
    }

    @ApiOperation(value = "게시글 추천", notes = "게시글을 추천할 수 있습니다")
    @PostMapping("/starroad/board/like")
    public ModelAndView handleLike(
            @ApiParam(value = "게시글 번호") @RequestParam("board") int boardNo,
            @ApiIgnore HttpSession session) {

        MemberDto memberDto = (MemberDto) session.getAttribute("currentUser");
        System.out.println("debug = " + memberDto.getId());

        if (memberDto.getId() == null ){

            ModelAndView mav = new ModelAndView("redirect:/starroad/login");
            mav.addObject("message", "로그인 후에 게시글을 작성할 수 있습니다.");
            return mav;

        } else if (!boardService.hasLiked(boardNo,memberDto.getId())) {

            ModelAndView mav= new ModelAndView("redirect:/starroad/board/detail?no=" + boardNo);
            mav.addObject("message", "로그인 후에 좋아요를 누르거나 이미 좋아요한 게시글입니다.");
            return mav;
        }

        else {

            boardService.increaseLikes(boardNo, memberDto.getId());
            ModelAndView mav= new ModelAndView("redirect:/starroad/board/detail?no=" + boardNo);
            return mav;
        }
    }

}
