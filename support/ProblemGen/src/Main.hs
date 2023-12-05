{-# OPTIONS_GHC -Wno-unrecognised-pragmas #-}

{-# HLINT ignore "Use tuple-section" #-}

module Main where

import Control.Monad.Random.Lazy
import Data.Map.Strict qualified as Map
import Data.Set qualified as Set
import Debug.Trace (trace)
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

-- Compute the (tree) height of a program
exprHeight :: ExprNode -> Int
exprHeight Const1 = 0
exprHeight Const2 = 0
exprHeight Const3 = 0
exprHeight VarX = 0
exprHeight VarY = 0
exprHeight VarZ = 0
exprHeight (Ite b t e) = 1 + foldl max (boolHeight b) [exprHeight t, exprHeight e]
exprHeight (Add e1 e2) = 1 + max (exprHeight e1) (exprHeight e2)
exprHeight (Multiply e1 e2) = 1 + max (exprHeight e1) (exprHeight e2)

boolHeight :: BoolNode -> Int
boolHeight (Lt e1 e2) = 1 + max (exprHeight e1) (exprHeight e2)
boolHeight (Eq e1 e2) = 1 + max (exprHeight e1) (exprHeight e2)
boolHeight (And b1 b2) = 1 + max (boolHeight b1) (boolHeight b2)
boolHeight (Or b1 b2) = 1 + max (boolHeight b1) (boolHeight b2)
boolHeight (Not b) = 1 + boolHeight b

-- Evaluate an expression (or program) given an environment
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

--
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
randomExpr minHeight maxHeight
  | minHeight >= (-1),
    maxHeight >= 0,
    minHeight <= maxHeight = do
      let newMinHeight = max (-1) (minHeight - 1)
          newMaxHeight = maxHeight - 1
      -- Height 0 by definition means terminal productions, they are height exactly 0 -- 1P means >= 1, 2P >= 2
      -- minHeight <= 0 means min height achieved, and that's the only circumstance we are allowed terminals
      exprGens1 <- if minHeight <= 0 then exprGenHeight0 newMinHeight newMaxHeight else return []
      exprGens2 <- if maxHeight >= 1 then exprGenHeight1P newMinHeight newMaxHeight else return []
      -- exprGens3 <- return []
      exprGens3 <- if maxHeight >= 2 then exprGenHeight2P newMinHeight newMaxHeight else return []
      let exprGens = exprGens1 ++ exprGens2 ++ exprGens3
      i <- getRandomR (0, length exprGens - 1)
      exprGens !! i

-- Generate an expression with height == 0 (i.e. a terminal production)
exprGenHeight0 :: (MonadRandom m) => Int -> Int -> m [m ExprNode]
exprGenHeight0 (-1) newMaxHeight
  | newMaxHeight >= (-1) =
      return
        [ return Const1,
          return Const2,
          return Const3,
          return VarX,
          return VarY,
          return VarZ
        ]

-- Generate an expression with height == 0 (i.e. a terminal)
exprGenHeight1P :: (MonadRandom m) => Int -> Int -> m [m ExprNode]
exprGenHeight1P newMinHeight newMaxHeight
  | newMaxHeight >= 0 = do
      newMins <- randomMinHeights newMinHeight 2
      let [newMinHeightA, newMinHeightB] = newMins
      return
        [ do
            e1 <- randomExpr newMinHeightA newMaxHeight
            e2 <- randomExpr newMinHeightB newMaxHeight
            return (Add e1 e2),
          do
            e1 <- randomExpr newMinHeightA newMaxHeight
            e2 <- randomExpr newMinHeightB newMaxHeight
            return (Multiply e1 e2)
        ]

exprGenHeight2P :: (MonadRandom m) => Int -> Int -> m [m ExprNode]
exprGenHeight2P newMinHeight newMaxHeight
  | newMaxHeight >= 1 = do
      newMins <- randomMinHeights newMinHeight 3
      let [newMinHeightA, newMinHeightB, newMinHeightC] = newMins
      return
        [ do
            b <- randomBool newMinHeightA newMaxHeight
            t <- randomExpr newMinHeightB newMaxHeight
            e <- randomExpr newMinHeightC newMaxHeight
            return (Ite b t e)
        ]

randomBool :: (MonadRandom m) => Int -> Int -> m BoolNode
randomBool minHeight maxHeight
  | minHeight >= (-1),
    maxHeight >= 0,
    minHeight <= maxHeight = do
      let newMinHeight = max (-1) (minHeight - 1)
          newMaxHeight = maxHeight - 1
      boolGens1 <- if maxHeight >= 1 then boolGenHeight1P newMinHeight newMaxHeight else return []
      boolGens2 <- if maxHeight >= 2 then boolGenHeight2P newMinHeight newMaxHeight else return []
      let boolGens = boolGens1 ++ boolGens2
      i <- getRandomR (0, length boolGens - 1)
      boolGens !! i

boolGenHeight1P :: (MonadRandom m) => Int -> Int -> m [m BoolNode]
boolGenHeight1P newMinHeight newMaxHeight
  | newMaxHeight >= 0 = do
      newMins <- randomMinHeights newMinHeight 2
      let [newMinHeightA, newMinHeightB] = newMins
      return
        [ do
            e1 <- randomExpr newMinHeightA newMaxHeight
            e2 <- randomExpr newMinHeightB newMaxHeight
            return (Lt e1 e2),
          do
            e1 <- randomExpr newMinHeightA newMaxHeight
            e2 <- randomExpr newMinHeightB newMaxHeight
            return (Eq e1 e2)
        ]

boolGenHeight2P :: (MonadRandom m) => Int -> Int -> m [m BoolNode]
boolGenHeight2P newMinHeight newMaxHeight
  | newMaxHeight >= 1 = do
      newMins <- randomMinHeights newMinHeight 2
      let [newMinHeightA, newMinHeightB] = newMins
      return
        [ do
            b1 <- randomBool newMinHeightA newMaxHeight
            b2 <- randomBool newMinHeightB newMaxHeight
            return (And b1 b2),
          do
            b1 <- randomBool newMinHeightA newMaxHeight
            b2 <- randomBool newMinHeightB newMaxHeight
            return (Or b1 b2),
          do
            b <- randomBool newMinHeight newMaxHeight
            return (Not b)
        ]

randomMinHeights :: (MonadRandom m, Random b, Num b) => b -> Int -> m [b]
randomMinHeights minHeight nBranches = do
  -- Only enforce minHeight on one branch -- others get random mins
  sel :: Int <- getRandomR (1, nBranches)
  mapM (\x -> if sel == x then return minHeight else getRandomR (0, minHeight)) [1 .. nBranches]

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
  nonUniqueExamples <- computeNonUniqueExamples (count - length uniqueExamples) program (Set.toList unusedEnvs)
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
      if Set.member result used || result < -2147483648 || result >= 2147483647
        then computeUniqueExamples count program remainEnvs used
        else do
          examples <- computeUniqueExamples (count - 1) program remainEnvs (Set.insert result used)
          return $ e : examples

    computeNonUniqueExamples :: (MonadRandom m) => Int -> ExprNode -> [Env] -> m [Env]
    -- computeNonUniqueExamples count envs | trace (printf "computeNonUniqueExamples %d <P> <E %d>" count (length envs)) False = undefined
    computeNonUniqueExamples 0 _ envs = return []
    computeNonUniqueExamples _ _ [] = return []
    computeNonUniqueExamples count program envs = do
      i <- getRandomR (0, length envs - 1)
      let e = envs !! i
      let remainEnvs = take i envs ++ drop (i + 1) envs
      let result = evalExpr e program
      if result < -2147483648 || result >= 2147483647
        then computeNonUniqueExamples count program remainEnvs
        else do
          examples <- computeNonUniqueExamples (count - 1) program remainEnvs
          return $ e : examples

describeExample :: Env -> ExprNode -> String
describeExample env@(Env x y z) program =
  printf "x=%d, y=%d, z=%d -> %d" x y z (evalExpr env program)

makeTestProgram :: (MonadRandom m) => Int -> Int -> m String
-- makeTestProgram height exampleCount | trace (printf "makeTestProgram %d %d" height exampleCount) False = undefined
makeTestProgram height exampleCount = do
  program <- randomExpr height height
  let tidyProgram = unfoldExpr $ foldExpr program
  if exprHeight tidyProgram < height
    then makeTestProgram height exampleCount
    else do
      let smallRangeCount = min 20 (max 2 (exampleCount `div` 2))
      smallRangeExamples <- bestExamples smallRangeCount tidyProgram smallRangeEnvs
      largeRangeExamples <- bestExamples (max 2 (exampleCount - smallRangeCount)) tidyProgram largeRangeEnvs
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
              programText <- makeTestProgram d 1000
              writeFile (printf "output/%02d%02d.txt" d i) programText
              print (d * 100 + i)
          )
          [0 .. 9]
    )
    [0 .. 19]
