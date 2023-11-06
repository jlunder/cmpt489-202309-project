{-# OPTIONS_GHC -Wno-unrecognised-pragmas #-}

{-# HLINT ignore "Use tuple-section" #-}

module Main where

import Control.Monad.Random.Lazy
import Data.Map.Strict qualified as Map
import Data.Set qualified as Set
-- import Debug.Trace (trace)
import System.IO (writeFile)
import Text.Printf (printf)

data ExprNode
  = Const1
  | Const2
  | Const3
  | VarX
  | VarY
  | VarZ
  | Ite BoolNode ExprNode ExprNode
  | Add ExprNode ExprNode
  | Multiply ExprNode ExprNode
  deriving (Eq, Show)

data BoolNode
  = Lt ExprNode ExprNode
  | Eq ExprNode ExprNode
  | And BoolNode BoolNode
  | Or BoolNode BoolNode
  | Not BoolNode
  deriving (Eq, Show)

data Env = Env {getX :: Integer, getY :: Integer, getZ :: Integer}
  deriving (Eq, Ord, Show)

programDepth :: ExprNode -> Int
programDepth =
  exprDepth
  where
    exprDepth :: ExprNode -> Int
    exprDepth Const1 = 1
    exprDepth Const2 = 1
    exprDepth Const3 = 1
    exprDepth VarX = 1
    exprDepth VarY = 1
    exprDepth VarZ = 1
    exprDepth (Ite b t e) = 1 + foldl max (boolDepth b) [exprDepth t, exprDepth e]
    exprDepth (Add e1 e2) = 1 + max (exprDepth e1) (exprDepth e2)
    exprDepth (Multiply e1 e2) = 1 + max (exprDepth e1) (exprDepth e2)

    boolDepth :: BoolNode -> Int
    boolDepth (Lt e1 e2) = 1 + max (exprDepth e1) (exprDepth e2)
    boolDepth (Eq e1 e2) = 1 + max (exprDepth e1) (exprDepth e2)
    boolDepth (And b1 b2) = 1 + max (boolDepth b1) (boolDepth b2)
    boolDepth (Or b1 b2) = 1 + max (boolDepth b1) (boolDepth b2)
    boolDepth (Not b) = 1 + boolDepth b

evalExpr :: Env -> ExprNode -> Integer
-- evalExpr env expr | trace (printf "evalExpr %s %s" (show env) (describeExpr expr)) False = undefined
evalExpr env Const1 = 1
evalExpr env Const2 = 2
evalExpr env Const3 = 3
evalExpr env VarX = getX env
evalExpr env VarY = getY env
evalExpr env VarZ = getZ env
evalExpr env (Ite b t e) = if evalBool env b then evalExpr env t else evalExpr env e
evalExpr env (Add e1 e2) = evalExpr env e1 + evalExpr env e2
evalExpr env (Multiply e1 e2) = evalExpr env e1 * evalExpr env e2

evalBool :: Env -> BoolNode -> Bool
evalBool env (Lt e1 e2) = evalExpr env e1 < evalExpr env e2
evalBool env (Eq e1 e2) = evalExpr env e1 == evalExpr env e2
evalBool env (And b1 b2) = evalBool env b1 && evalBool env b2
evalBool env (Or b1 b2) = evalBool env b1 || evalBool env b2
evalBool env (Not b) = not (evalBool env b)

foldExpr :: ExprNode -> Either ExprNode Integer
-- foldExpr e | trace ("foldExpr " ++ describeExpr e) False = undefined
foldExpr Const1 = Right 1
foldExpr Const2 = Right 2
foldExpr Const3 = Right 3
foldExpr VarX = Left VarX
foldExpr VarY = Left VarY
foldExpr VarZ = Left VarZ
foldExpr (Ite b t e) =
  let cb = foldBool b
      ct = foldExpr t
      ce = foldExpr e
   in case tryEvalBool cb of
        Just bv -> if bv then ct else ce
        Nothing ->
          if ct == ce
            then Left $ unfoldExpr ct
            else Left $ Ite cb (unfoldExpr ct) (unfoldExpr ce)
foldExpr (Ite b t e) =
  let cb = foldBool b
      ct = foldExpr t
      ce = foldExpr e
   in case tryEvalBool cb of
        Just bv -> if bv then ct else ce
        Nothing -> Left $ Ite cb (unfoldExpr ct) (unfoldExpr ce)
foldExpr (Add e1 e2) =
  let ce1 = foldExpr e1
      ce2 = foldExpr e2
   in case (ce1, ce2) of
        (Right v1, Right v2) -> Right (v1 + v2)
        (Left ef1, Right v2) -> Left $ Add ef1 (unfoldExpr $ Right v2)
        (Right v1, Left ef2) -> Left $ Add (unfoldExpr $ Right v1) ef2
        (Left ef1, Left ef2) -> Left $ Add ef1 ef2
foldExpr (Multiply Const1 e2) = foldExpr e2
foldExpr (Multiply e1 Const1) = foldExpr e1
foldExpr (Multiply e1 e2) =
  let ce1 = foldExpr e1
      ce2 = foldExpr e2
   in case (ce1, ce2) of
        (Right v1, Right v2) -> Right (v1 * v2)
        (Left ef1, Right v2) -> Left $ Multiply ef1 (unfoldExpr $ Right v2)
        (Right v1, Left ef2) -> Left $ Multiply (unfoldExpr $ Right v1) ef2
        (Left ef1, Left ef2) -> Left $ Multiply ef1 ef2

unfoldExpr :: Either ExprNode Integer -> ExprNode
-- unfoldExpr e | trace ("unfoldExpr " ++ show e) False = undefined
unfoldExpr (Left e) = e
unfoldExpr (Right 1) = Const1
unfoldExpr (Right 2) = Const2
unfoldExpr (Right 3) = Const3
unfoldExpr (Right v) | v > 3 = Add (unfoldExpr $ Right (v - 3)) Const3

foldBool :: BoolNode -> BoolNode
-- foldBool b | trace ("foldBool " ++ describeBool b) False = undefined
foldBool (Lt e1 e2) = Lt (unfoldExpr $ foldExpr e1) (unfoldExpr $ foldExpr e2)
foldBool (Eq e1 e2) = Eq (unfoldExpr $ foldExpr e1) (unfoldExpr $ foldExpr e2)
foldBool (And b1 b2) = case tryEvalBool b1 of
  Just True -> foldBool b2
  _ -> case tryEvalBool b2 of
    Just True -> foldBool b1
    _ -> And (foldBool b1) (foldBool b2)
foldBool (Or b1 b2) = case tryEvalBool b1 of
  Just False -> foldBool b2
  _ -> case tryEvalBool b2 of
    Just False -> foldBool b1
    _ -> Or (foldBool b1) (foldBool b2)
foldBool (Not (Not b)) = b
foldBool (Not b) = Not (foldBool b)

tryEvalBool :: BoolNode -> Maybe Bool
tryEvalBool (Lt e1 e2) = do
  v1 <- either (const Nothing) Just (foldExpr e1)
  v2 <- either (const Nothing) Just (foldExpr e2)
  return $ v1 < v2
tryEvalBool (Eq e1 e2) = do
  v1 <- either (const Nothing) Just (foldExpr e1)
  v2 <- either (const Nothing) Just (foldExpr e2)
  return $ v1 == v2
tryEvalBool (And b1 b2) =
  case tryEvalBool b1 of
    Just False -> Just False
    _ -> tryEvalBool b2
tryEvalBool (Or b1 b2) =
  case tryEvalBool b1 of
    Just True -> Just True
    _ -> tryEvalBool b2
tryEvalBool (Not b) = not <$> tryEvalBool b

describeExpr :: ExprNode -> String
describeExpr Const1 = "1"
describeExpr Const2 = "2"
describeExpr Const3 = "3"
describeExpr VarX = "x"
describeExpr VarY = "y"
describeExpr VarZ = "z"
describeExpr (Ite b t e) =
  "Ite(" ++ describeBool b ++ ", " ++ describeExpr t ++ ", " ++ describeExpr e ++ ")"
describeExpr (Add e1 e2) = "Add(" ++ describeExpr e1 ++ ", " ++ describeExpr e2 ++ ")"
describeExpr (Multiply e1 e2) = "Multiply(" ++ describeExpr e1 ++ ", " ++ describeExpr e2 ++ ")"

describeBool :: BoolNode -> String
describeBool (Lt e1 e2) = "Lt(" ++ describeExpr e1 ++ ", " ++ describeExpr e2 ++ ")"
describeBool (Eq e1 e2) = "Eq(" ++ describeExpr e1 ++ ", " ++ describeExpr e2 ++ ")"
describeBool (And b1 b2) = "And(" ++ describeBool b1 ++ ", " ++ describeBool b2 ++ ")"
describeBool (Or b1 b2) = "Or(" ++ describeBool b1 ++ ", " ++ describeBool b2 ++ ")"
describeBool (Not b) = "Not(" ++ describeBool b ++ ")"

randomExpr :: (MonadRandom m) => Int -> Int -> m ExprNode
randomExpr minDepth maxDepth
  | minDepth >= 0,
    maxDepth >= 1,
    minDepth <= maxDepth = do
      let newMinDepth = max 0 (minDepth - 1)
          newMaxDepth = maxDepth - 1
      exprGens1 <- if minDepth <= 1 then exprGenDepth1 newMinDepth newMaxDepth else return []
      exprGens2 <- if maxDepth >= 2 then exprGenDepth2P newMinDepth newMaxDepth else return []
      exprGens3 <- if maxDepth >= 3 then exprGenDepth3P newMinDepth newMaxDepth else return []
      let exprGens = exprGens1 ++ exprGens2 ++ exprGens3
      i <- getRandomR (0, length exprGens - 1)
      exprGens !! i

exprGenDepth1 :: (MonadRandom m) => Int -> Int -> m [m ExprNode]
exprGenDepth1 0 newMaxDepth
  | newMaxDepth >= 0 =
      return
        [ return Const1,
          return Const2,
          return Const3,
          return VarX,
          return VarY,
          return VarZ
        ]

exprGenDepth2P :: (MonadRandom m) => Int -> Int -> m [m ExprNode]
exprGenDepth2P newMinDepth newMaxDepth
  | newMaxDepth >= 1 = do
      sel :: Int <- getRandomR (1, 2)
      let newMinDepthA = if sel == 1 then newMinDepth else 0
          newMinDepthB = if sel == 2 then newMinDepth else 0
      return
        [ do
            e1 <- randomExpr newMinDepthA newMaxDepth
            e2 <- randomExpr newMinDepthB newMaxDepth
            return (Add e1 e2),
          do
            e1 <- randomExpr newMinDepthA newMaxDepth
            e2 <- randomExpr newMinDepthB newMaxDepth
            return (Multiply e1 e2)
        ]

exprGenDepth3P :: (MonadRandom m) => Int -> Int -> m [m ExprNode]
exprGenDepth3P newMinDepth newMaxDepth
  | newMaxDepth >= 2 = do
      sel :: Int <- getRandomR (1, 3)
      let newMinDepthA = if sel == 1 then newMinDepth else 0
          newMinDepthB = if sel == 2 then newMinDepth else 0
          newMinDepthC = if sel == 3 then newMinDepth else 0
      return
        [ do
            b <- randomBool newMinDepthA newMaxDepth
            t <- randomExpr newMinDepthB newMaxDepth
            e <- randomExpr newMinDepthC newMaxDepth
            return (Ite b t e)
        ]

randomBool :: (MonadRandom m) => Int -> Int -> m BoolNode
randomBool minDepth maxDepth
  | minDepth >= 0,
    maxDepth >= 1,
    minDepth <= maxDepth = do
      let newMinDepth = max 0 (minDepth - 1)
          newMaxDepth = maxDepth - 1
      boolGens1 <- if maxDepth >= 2 then boolGenDepth2P newMinDepth newMaxDepth else return []
      boolGens2 <- if maxDepth >= 3 then boolGenDepth3P newMinDepth newMaxDepth else return []
      let boolGens = boolGens1 ++ boolGens2
      i <- getRandomR (0, length boolGens - 1)
      boolGens !! i

boolGenDepth2P :: (MonadRandom m) => Int -> Int -> m [m BoolNode]
boolGenDepth2P newMinDepth newMaxDepth
  | newMaxDepth >= 1 = do
      sel :: Int <- getRandomR (1, 2)
      let newMinDepthA = if sel == 1 then newMinDepth else 0
          newMinDepthB = if sel == 2 then newMinDepth else 0
      return
        [ do
            e1 <- randomExpr newMinDepthA newMaxDepth
            e2 <- randomExpr newMinDepthB newMaxDepth
            return (Lt e1 e2),
          do
            e1 <- randomExpr newMinDepthA newMaxDepth
            e2 <- randomExpr newMinDepthB newMaxDepth
            return (Eq e1 e2)
        ]

boolGenDepth3P :: (MonadRandom m) => Int -> Int -> m [m BoolNode]
boolGenDepth3P newMinDepth newMaxDepth
  | newMaxDepth >= 2 = do
      sel :: Int <- getRandomR (1, 2)
      let newMinDepthA = if sel == 1 then newMinDepth else 0
          newMinDepthB = if sel == 2 then newMinDepth else 0
      return
        [ do
            b1 <- randomBool newMinDepthA newMaxDepth
            b2 <- randomBool newMinDepthB newMaxDepth
            return (And b1 b2),
          do
            b1 <- randomBool newMinDepthA newMaxDepth
            b2 <- randomBool newMinDepthB newMaxDepth
            return (Or b1 b2),
          do
            b <- randomBool newMinDepth newMaxDepth
            return (Not b)
        ]

allEnvs :: Integer -> Integer -> [Env]
allEnvs rangeMin rangeMax =
  let range = [rangeMin .. rangeMax]
   in concatMap (\i -> concatMap (\j -> map (Env i j) range) range) range

randomEnv :: (MonadRandom m) => Integer -> Integer -> m Env
randomEnv rangeMin rangeMax = do
  x <- getRandomR (rangeMin, rangeMax)
  y <- getRandomR (rangeMin, rangeMax)
  z <- getRandomR (rangeMin, rangeMax)
  return Env {getX = x, getY = y, getZ = z}

bestExamples :: (MonadRandom m) => Int -> ExprNode -> Set.Set Env -> m [Env]
-- bestExamples count program envs | trace (printf "bestExamples %d <P> <E %d>" count (length envs)) False = undefined
bestExamples count program envs = do
  uniqueExamples <- computeUniqueExamples count program (Set.toList envs) Set.empty
  let unusedEnvs = Set.difference envs (Set.fromList uniqueExamples)
  nonUniqueExamples <- computeNonUniqueExamples (count - length uniqueExamples) (Set.toList unusedEnvs)
  return $ uniqueExamples ++ nonUniqueExamples
  where
    computeUniqueExamples :: (MonadRandom m) => Int -> ExprNode -> [Env] -> Set.Set Integer -> m [Env]
    -- computeUniqueExamples count program envs used | trace (printf "computeUniqueExamples %d %s <E %d> <U %d>" count (describeExpr program) (length envs) (Set.size used)) False = undefined
    computeUniqueExamples 0 _ _ _ = return []
    computeUniqueExamples _ _ [] _ = return []
    computeUniqueExamples count program envs used = do
      i <- getRandomR (0, length envs - 1)
      let e = envs !! i
      let remainEnvs = take i envs ++ drop (i + 1) envs
      let result = evalExpr e program
      if Set.member result used
        then computeUniqueExamples count program remainEnvs used
        else do
          examples <- computeUniqueExamples (count - 1) program remainEnvs (Set.insert result used)
          return $ e : examples

    computeNonUniqueExamples :: (MonadRandom m) => Int -> [Env] -> m [Env]
    -- computeNonUniqueExamples count envs | trace (printf "computeNonUniqueExamples %d <P> <E %d>" count (length envs)) False = undefined
    computeNonUniqueExamples 0 envs = return []
    computeNonUniqueExamples count envs = do
      i <- getRandomR (0, length envs - 1)
      let e = envs !! i
      let remainEnvs = take i envs ++ drop (i + 1) envs
      examples <- computeNonUniqueExamples (count - 1) remainEnvs
      return $ e : examples

describeExample :: Env -> ExprNode -> String
describeExample env@(Env x y z) program =
  printf "x=%d, y=%d, z=%d -> %d" x y z (evalExpr env program)

makeTestProgram :: (MonadRandom m) => Int -> Int -> m String
-- makeTestProgram depth exampleCount | trace (printf "makeTestProgram %d %d" depth exampleCount) False = undefined
makeTestProgram depth exampleCount = do
  program <- randomExpr depth depth
  let tidyProgram = unfoldExpr $ foldExpr program
  if programDepth tidyProgram < depth
    then makeTestProgram depth exampleCount
    else do
      smallRangeExamples <- bestExamples (max 2 (exampleCount - (exampleCount `div` 2))) tidyProgram smallRangeEnvs
      largeRangeExamples <- bestExamples (max 2 (exampleCount `div` 2)) tidyProgram largeRangeEnvs
      let examples = smallRangeExamples ++ largeRangeExamples
      let examplesText =
            printf "# %s\n" (describeExpr tidyProgram)
              ++ concatMap (\ex -> describeExample ex tidyProgram ++ "\n") examples
      return examplesText
  where
    smallRangeEnvs = Set.fromList (allEnvs 1 3)
    largeRangeEnvs = Set.difference (Set.fromList (allEnvs (-5) 15)) smallRangeEnvs

main :: IO ()
main = do
  mapM_
    ( \d ->
        mapM_
          ( \(i :: Int) -> do
              programText <- makeTestProgram d (d * 2)
              writeFile (printf "../%02d%02d.txt" d i) programText
          )
          [0 .. (if d < 4 then 19 else 9)]
    )
    [2 .. 7]