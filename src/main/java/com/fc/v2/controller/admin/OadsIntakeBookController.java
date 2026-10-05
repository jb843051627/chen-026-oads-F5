package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.exception.spot.SpotRuleException;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.custom.IntakeBookForm;
import com.fc.v2.service.intake.ITOadsIntakeService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 牌匾更换名册·区局收讫环节门面。
 *
 * <p>四步（点行数→验数据→定去向→出回执）全在 {@link ITOadsIntakeService} 一处连着做完，
 * 控制器只收整册报文、回原句交代；不接零散逐行报送，原先五检查点那一套一点没动。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Api(value = "牌匾名册收讫")
@Controller
@RequestMapping("/oadsIntakeBook")
public class OadsIntakeBookController extends BaseController {

    private final String prefix = "admin/oadsIntakeBook";

    @Autowired
    private ITOadsIntakeService intakeService;

    @ApiOperation(value = "收讫跳转", notes = "收讫跳转")
    @GetMapping("/view")
    @RequiresPermissions("oads:oadsIntakeBook:view")
    public String view(ModelMap model) {
        model.put("defaultRowLimit", 200);
        return prefix + "/list";
    }

    @Log(title = "牌匾名册整册收讫", action = "receive")
    @ApiOperation(value = "整册收讫", notes = "一册一次交完整，四步连着做完出回执")
    @PostMapping("/receive")
    @RequiresPermissions("oads:oadsIntakeBook:receive")
    @ResponseBody
    public AjaxResult receive(@RequestBody IntakeBookForm form) {
        try {
            return AjaxResult.successData(200, intakeService.receive(form, ShiroUtils.getLoginName()))
                    .put("msg", "收讫四步已连着做完");
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @ApiOperation(value = "回执回查", notes = "三数回库逐行重数，逐行互印点名列差异")
    @GetMapping("/receipt")
    @RequiresPermissions("oads:oadsIntakeBook:list")
    @ResponseBody
    public AjaxResult receipt(@RequestParam("bookNo") String bookNo) {
        try {
            return AjaxResult.successData(200, intakeService.receipt(bookNo));
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "收讫缺照补挂", action = "attachPhoto")
    @PostMapping("/attachPhoto")
    @RequiresPermissions("oads:oadsIntakeBook:photo")
    @ResponseBody
    public AjaxResult attachPhoto(@RequestParam("bookNo") String bookNo,
                                  @RequestParam("rowNo") int rowNo,
                                  @RequestParam("photoNo") String photoNo) {
        try {
            return AjaxResult.successData(200,
                    intakeService.attachPhoto(bookNo, rowNo, photoNo, ShiroUtils.getLoginName()))
                    .put("msg", "照片已补挂，本行重走验数据、定去向");
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "销账流水对账", action = "reconcile")
    @PostMapping("/reconcile")
    @RequiresPermissions("oads:oadsIntakeBook:reconcile")
    @ResponseBody
    public AjaxResult reconcile(@RequestParam("bookNo") String bookNo) {
        try {
            return AjaxResult.successData(200, intakeService.reconcile(bookNo, ShiroUtils.getLoginName()));
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "把收讫册划入历史层", action = "markHistory")
    @PostMapping("/markHistory")
    @RequiresPermissions("oads:oadsIntakeBook:archive")
    @ResponseBody
    public AjaxResult markHistory(@RequestParam("bookNo") String bookNo) {
        try {
            intakeService.markHistory(bookNo, ShiroUtils.getLoginName());
            return AjaxResult.success("册 " + bookNo + " 已划入历史层（行一条不删，三数不变）");
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "历史册待核两年归档", action = "archive")
    @PostMapping("/archive")
    @RequiresPermissions("oads:oadsIntakeBook:archive")
    @ResponseBody
    public AjaxResult archive(@RequestParam(value = "asOf", required = false) String asOf) {
        try {
            Date at = null;
            if (asOf != null && !asOf.trim().isEmpty()) {
                at = new SimpleDateFormat("yyyy-MM-dd").parse(asOf.trim());
            }
            return AjaxResult.successData(200, intakeService.archive(at, ShiroUtils.getLoginName()));
        } catch (java.text.ParseException e) {
            return AjaxResult.error("判定当日格式应为 yyyy-MM-dd：" + asOf);
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
