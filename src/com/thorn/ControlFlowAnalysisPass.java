package com.thorn;

import java.util.*;

/**
 * Analyzes control flow patterns to enable other optimizations.
 * This is an analysis pass that gathers information about:
 * - Basic blocks and control flow graph
 * - Loop structures
 * - Reachability
 * - Live variables
 */
public class ControlFlowAnalysisPass extends OptimizationPass {
    
    @Override
    public String getName() {
        return "control-flow-analysis";
    }
    
    @Override
    public PassType getType() {
        return PassType.ANALYSIS;
    }
    
    @Override
    public OptimizationLevel getMinimumLevel() {
        return OptimizationLevel.O1;
    }
    
    @Override
    public List<String> getDependencies() {
        return Collections.emptyList(); // No dependencies - runs first
    }
    
    @Override
    public List<Stmt> optimize(List<Stmt> statements, OptimizationContext context) {
        if (context.isDebugMode()) {
            System.out.println("=== Control Flow Analysis Pass ===");
        }
        
        ControlFlowGraph cfg = buildControlFlowGraph(statements);
        ReachabilityInfo reachability = analyzeReachability(cfg);
        LoopInfo loops = findLoops(cfg);
        
        // Cache analysis results for other passes
        context.cacheAnalysis("control-flow-graph", cfg);
        context.cacheAnalysis("reachability-info", reachability);
        context.cacheAnalysis("loop-info", loops);
        
        if (context.isDebugMode()) {
            System.out.println("  Basic blocks: " + cfg.getBasicBlocks().size());
            System.out.println("  Loops found: " + loops.getLoops().size());
            System.out.println("  Unreachable blocks: " + reachability.getUnreachableBlocks().size());
        }
        
        // Analysis pass - returns statements unchanged
        return statements;
    }
    
    private ControlFlowGraph buildControlFlowGraph(List<Stmt> statements) {
        ControlFlowGraph cfg = new ControlFlowGraph();
        CFGBuilder builder = new CFGBuilder(cfg);
        builder.build(statements);
        return cfg;
    }
    
    private ReachabilityInfo analyzeReachability(ControlFlowGraph cfg) {
        ReachabilityAnalyzer analyzer = new ReachabilityAnalyzer();
        return analyzer.analyze(cfg);
    }
    
    private LoopInfo findLoops(ControlFlowGraph cfg) {
        LoopFinder finder = new LoopFinder();
        return finder.findLoops(cfg);
    }
    
    /**
     * Represents the control flow graph.
     */
    public static class ControlFlowGraph {
        private final List<BasicBlock> blocks = new ArrayList<>();
        private BasicBlock entryBlock;
        private BasicBlock exitBlock;
        
        public void addBlock(BasicBlock block) {
            blocks.add(block);
        }
        
        public List<BasicBlock> getBasicBlocks() {
            return Collections.unmodifiableList(blocks);
        }
        
        public BasicBlock getEntryBlock() {
            return entryBlock;
        }
        
        public void setEntryBlock(BasicBlock block) {
            this.entryBlock = block;
        }
        
        public BasicBlock getExitBlock() {
            return exitBlock;
        }
        
        public void setExitBlock(BasicBlock block) {
            this.exitBlock = block;
        }
    }
    
    /**
     * Represents a basic block in the control flow graph.
     */
    public static class BasicBlock {
        private final int id;
        private final List<Stmt> statements = new ArrayList<>();
        private final List<BasicBlock> successors = new ArrayList<>();
        private final List<BasicBlock> predecessors = new ArrayList<>();
        
        public BasicBlock(int id) {
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
        
        public void addStatement(Stmt stmt) {
            statements.add(stmt);
        }
        
        public List<Stmt> getStatements() {
            return Collections.unmodifiableList(statements);
        }
        
        public void addSuccessor(BasicBlock block) {
            successors.add(block);
            block.predecessors.add(this);
        }
        
        public List<BasicBlock> getSuccessors() {
            return Collections.unmodifiableList(successors);
        }
        
        public List<BasicBlock> getPredecessors() {
            return Collections.unmodifiableList(predecessors);
        }
    }
    
    /**
     * Builds the control flow graph from statements.
     */
    private static class CFGBuilder {
        private final ControlFlowGraph cfg;
        private int nextBlockId = 0;
        private BasicBlock currentBlock;
        
        public CFGBuilder(ControlFlowGraph cfg) {
            this.cfg = cfg;
        }
        
        public void build(List<Stmt> statements) {
            BasicBlock entry = new BasicBlock(nextBlockId++);
            cfg.setEntryBlock(entry);
            cfg.addBlock(entry);
            currentBlock = entry;
            
            for (Stmt stmt : statements) {
                processStatement(stmt);
            }
            
            // Create exit block
            BasicBlock exit = new BasicBlock(nextBlockId++);
            cfg.setExitBlock(exit);
            cfg.addBlock(exit);
            
            // Connect blocks without explicit successors to exit
            for (BasicBlock block : cfg.getBasicBlocks()) {
                if (block != exit && block.getSuccessors().isEmpty() && 
                    !endsWithReturn(block)) {
                    block.addSuccessor(exit);
                }
            }
        }
        
        private void processStatement(Stmt stmt) {
            if (stmt instanceof Stmt.If) {
                processIfStatement((Stmt.If) stmt);
            } else if (stmt instanceof Stmt.While) {
                processWhileStatement((Stmt.While) stmt);
            } else if (stmt instanceof Stmt.Return) {
                currentBlock.addStatement(stmt);
                // Return statements don't have successors
                currentBlock = new BasicBlock(nextBlockId++);
                cfg.addBlock(currentBlock);
            } else if (stmt instanceof Stmt.Block) {
                Stmt.Block block = (Stmt.Block) stmt;
                for (Stmt s : block.statements) {
                    processStatement(s);
                }
            } else {
                currentBlock.addStatement(stmt);
            }
        }
        
        private void processIfStatement(Stmt.If stmt) {
            currentBlock.addStatement(stmt);
            
            BasicBlock thenBlock = new BasicBlock(nextBlockId++);
            BasicBlock elseBlock = stmt.elseBranch != null ? 
                new BasicBlock(nextBlockId++) : null;
            BasicBlock mergeBlock = new BasicBlock(nextBlockId++);
            
            cfg.addBlock(thenBlock);
            if (elseBlock != null) cfg.addBlock(elseBlock);
            cfg.addBlock(mergeBlock);
            
            currentBlock.addSuccessor(thenBlock);
            if (elseBlock != null) {
                currentBlock.addSuccessor(elseBlock);
            } else {
                currentBlock.addSuccessor(mergeBlock);
            }
            
            // Process then branch
            currentBlock = thenBlock;
            processStatement(stmt.thenBranch);
            if (!endsWithReturn(currentBlock)) {
                currentBlock.addSuccessor(mergeBlock);
            }
            
            // Process else branch
            if (elseBlock != null) {
                currentBlock = elseBlock;
                processStatement(stmt.elseBranch);
                if (!endsWithReturn(currentBlock)) {
                    currentBlock.addSuccessor(mergeBlock);
                }
            }
            
            currentBlock = mergeBlock;
        }
        
        private void processWhileStatement(Stmt.While stmt) {
            BasicBlock conditionBlock = new BasicBlock(nextBlockId++);
            BasicBlock bodyBlock = new BasicBlock(nextBlockId++);
            BasicBlock exitBlock = new BasicBlock(nextBlockId++);
            
            cfg.addBlock(conditionBlock);
            cfg.addBlock(bodyBlock);
            cfg.addBlock(exitBlock);
            
            currentBlock.addSuccessor(conditionBlock);
            
            conditionBlock.addStatement(stmt);
            conditionBlock.addSuccessor(bodyBlock);
            conditionBlock.addSuccessor(exitBlock);
            
            currentBlock = bodyBlock;
            processStatement(stmt.body);
            if (!endsWithReturn(currentBlock)) {
                currentBlock.addSuccessor(conditionBlock);
            }
            
            currentBlock = exitBlock;
        }
        
        private boolean endsWithReturn(BasicBlock block) {
            List<Stmt> stmts = block.getStatements();
            return !stmts.isEmpty() && 
                   stmts.get(stmts.size() - 1) instanceof Stmt.Return;
        }
    }
    
    /**
     * Information about reachability in the control flow graph.
     */
    public static class ReachabilityInfo {
        private final Set<BasicBlock> reachableBlocks = new HashSet<>();
        private final Set<BasicBlock> unreachableBlocks = new HashSet<>();
        
        public void markReachable(BasicBlock block) {
            reachableBlocks.add(block);
        }
        
        public void markUnreachable(BasicBlock block) {
            unreachableBlocks.add(block);
        }
        
        public boolean isReachable(BasicBlock block) {
            return reachableBlocks.contains(block);
        }
        
        public Set<BasicBlock> getUnreachableBlocks() {
            return Collections.unmodifiableSet(unreachableBlocks);
        }
    }
    
    /**
     * Analyzes reachability in the control flow graph.
     */
    private static class ReachabilityAnalyzer {
        public ReachabilityInfo analyze(ControlFlowGraph cfg) {
            ReachabilityInfo info = new ReachabilityInfo();
            
            // DFS from entry block
            Set<BasicBlock> visited = new HashSet<>();
            dfs(cfg.getEntryBlock(), visited, info);
            
            // Mark unvisited blocks as unreachable
            for (BasicBlock block : cfg.getBasicBlocks()) {
                if (!visited.contains(block)) {
                    info.markUnreachable(block);
                }
            }
            
            return info;
        }
        
        private void dfs(BasicBlock block, Set<BasicBlock> visited, 
                        ReachabilityInfo info) {
            if (block == null || visited.contains(block)) {
                return;
            }
            
            visited.add(block);
            info.markReachable(block);
            
            for (BasicBlock successor : block.getSuccessors()) {
                dfs(successor, visited, info);
            }
        }
    }
    
    /**
     * Information about loops in the control flow graph.
     */
    public static class LoopInfo {
        private final List<Loop> loops = new ArrayList<>();
        
        public void addLoop(Loop loop) {
            loops.add(loop);
        }
        
        public List<Loop> getLoops() {
            return Collections.unmodifiableList(loops);
        }
    }
    
    /**
     * Represents a loop in the control flow graph.
     */
    public static class Loop {
        private final BasicBlock header;
        private final Set<BasicBlock> blocks = new HashSet<>();
        
        public Loop(BasicBlock header) {
            this.header = header;
        }
        
        public BasicBlock getHeader() {
            return header;
        }
        
        public void addBlock(BasicBlock block) {
            blocks.add(block);
        }
        
        public Set<BasicBlock> getBlocks() {
            return Collections.unmodifiableSet(blocks);
        }
    }
    
    /**
     * Finds loops in the control flow graph.
     */
    private static class LoopFinder {
        public LoopInfo findLoops(ControlFlowGraph cfg) {
            LoopInfo info = new LoopInfo();
            
            // Find back edges
            Set<BasicBlock> visited = new HashSet<>();
            Set<BasicBlock> inStack = new HashSet<>();
            
            for (BasicBlock block : cfg.getBasicBlocks()) {
                if (!visited.contains(block)) {
                    findLoopsFrom(block, visited, inStack, info);
                }
            }
            
            return info;
        }
        
        private void findLoopsFrom(BasicBlock block, Set<BasicBlock> visited,
                                  Set<BasicBlock> inStack, LoopInfo info) {
            visited.add(block);
            inStack.add(block);
            
            for (BasicBlock successor : block.getSuccessors()) {
                if (!visited.contains(successor)) {
                    findLoopsFrom(successor, visited, inStack, info);
                } else if (inStack.contains(successor)) {
                    // Found a back edge - successor is loop header
                    Loop loop = new Loop(successor);
                    collectLoopBlocks(block, successor, loop);
                    info.addLoop(loop);
                }
            }
            
            inStack.remove(block);
        }
        
        private void collectLoopBlocks(BasicBlock from, BasicBlock header, Loop loop) {
            if (from == header) {
                loop.addBlock(header);
                return;
            }
            
            Set<BasicBlock> visited = new HashSet<>();
            collectLoopBlocksHelper(from, header, loop, visited);
            loop.addBlock(header);
        }
        
        private boolean collectLoopBlocksHelper(BasicBlock current, BasicBlock header,
                                               Loop loop, Set<BasicBlock> visited) {
            if (current == header) {
                return true;
            }
            
            if (visited.contains(current)) {
                return false;
            }
            
            visited.add(current);
            
            for (BasicBlock pred : current.getPredecessors()) {
                if (collectLoopBlocksHelper(pred, header, loop, visited)) {
                    loop.addBlock(current);
                    return true;
                }
            }
            
            return false;
        }
    }
}