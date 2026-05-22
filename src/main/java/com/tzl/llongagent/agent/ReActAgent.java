package com.tzl.llongagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/***
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考 - 行动的循环模式
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ReActAgent extends BaseAgent {

    public abstract boolean think();

    public abstract String act();

    public abstract boolean thinkStream();

    public abstract String actStream();

    @Override
    public String step() {

        // 这里没有加　try - catch　
        // 因为父类 BaseAgent　需要拿到报错，才能停止，才能把 state = AgentState.ERROR
        // 如果这里把报错吞了，那么上层拿不到报错，就无法因为错误而中断循环!

        boolean shouldAct = think();
        if(!shouldAct) {
            return "思考完成 - 无需行动！";
        }
        return act();

    }

    @Override
    public String stepStream(){

        boolean shouldAct = thinkStream();
        if(!shouldAct) {
            return "思考完成 - 无需行动！";
        }
        return actStream();
        
    }

}
